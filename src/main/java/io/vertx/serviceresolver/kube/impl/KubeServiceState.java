/*
 * Copyright (c) 2011-2023 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package io.vertx.serviceresolver.kube.impl;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.WebSocket;
import io.vertx.core.http.WebSocketConnectOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.spi.endpoint.EndpointBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

class KubeServiceState<B> {

  final String name;
  final Vertx vertx;
  final KubeResolverImpl resolver;
  final EndpointBuilder<B, SocketAddress> endpointsBuilder;
  String lastResourceVersion;
  boolean disposed;
  WebSocket ws;
  AtomicReference<B> endpoints = new AtomicReference<>();

  KubeServiceState(EndpointBuilder<B, SocketAddress> endpointsBuilder, KubeResolverImpl resolver, Vertx vertx, String lastResourceVersion, String name) {
    this.endpointsBuilder = endpointsBuilder;
    this.name = name;
    this.resolver = resolver;
    this.vertx = vertx;
    this.lastResourceVersion = lastResourceVersion;
  }

  void connectWebSocket() {
    String requestURI = "/api/v1/namespaces/" + resolver.namespace + "/endpoints?"
      + "watch=true"
      + "&"
      + "allowWatchBookmarks=true"
      + "&"
      + "resourceVersion=" + lastResourceVersion;
    WebSocketConnectOptions connectOptions = new WebSocketConnectOptions();
    connectOptions.setServer(resolver.server);
    connectOptions.setURI(requestURI);
    if (resolver.bearerToken != null) {
      connectOptions.putHeader(HttpHeaders.AUTHORIZATION, "Bearer " + resolver.bearerToken);
    }
    resolver.wsClient.webSocket()
      .handler(buff -> {
        JsonObject update  = buff.toJsonObject();
        handleUpdate(update);
      })
      .closeHandler(v -> {
        if (!disposed) {
          connectWebSocket();
        }
      }).connect(connectOptions).onComplete(ar -> {
        if (ar.succeeded()) {
          WebSocket ws = ar.result();
          if (disposed) {
            ws.close();
          } else {
            this.ws = ws;
          }
        } else {
          if (!disposed) {
            // Retry WebSocket connect
            vertx.setTimer(500, id -> {
              connectWebSocket();
            });
          }
        }
      });
  }

  void handleUpdate(JsonObject update) {
    String type = update.getString("type");
    JsonObject object = update.getJsonObject("object");
    JsonObject metadata = object.getJsonObject("metadata");
    String resourceVersion = metadata.getString("resourceVersion");
    if (!lastResourceVersion.equals(resourceVersion)) {
      handleEndpoints(object);
    }
  }

  void handleEndpoints(JsonObject item) {
    JsonObject metadata = item.getJsonObject("metadata");
    String name = metadata.getString("name");
    if (this.name.equals(name)) {
      JsonArray subsets = item.getJsonArray("subsets");
      EndpointBuilder<B, SocketAddress> builder = endpointsBuilder;
      if (subsets != null) {
        for (int j = 0;j < subsets.size();j++) {
          List<String> podIps = new ArrayList<>();
          JsonObject subset = subsets.getJsonObject(j);
          JsonArray addresses = subset.getJsonArray("addresses");
          JsonArray ports = subset.getJsonArray("ports");
          for (int k = 0;k < addresses.size();k++) {
            JsonObject address = addresses.getJsonObject(k);
            String ip = address.getString("ip");
            podIps.add(ip);
          }
          for (int k = 0;k < ports.size();k++) {
            JsonObject port = ports.getJsonObject(k);
            int podPort = port.getInteger("port");
            for (String podIp : podIps) {
              SocketAddress podAddress = SocketAddress.inetSocketAddress(podPort, podIp);
              builder = builder.addServer(podAddress, podIp + "-" + podPort);
            }
          }
        }
      }
      this.endpoints.set(builder.build());
    }
  }
}
