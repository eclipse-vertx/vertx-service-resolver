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

import io.vertx.core.http.WebSocket;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.spi.endpoint.EndpointBuilder;
import io.vertx.serviceresolver.ServiceAddress;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

class KubeServiceState<B> {

  final ServiceAddress address;
  final String name;
  final EndpointBuilder<B, SocketAddress> endpointsBuilder;
  boolean disposed;
  WebSocket webSocket;
  AtomicReference<B> endpoints = new AtomicReference<>();
  volatile boolean valid;

  KubeServiceState(EndpointBuilder<B, SocketAddress> endpointsBuilder, ServiceAddress address, String name) {
    this.endpointsBuilder = endpointsBuilder;
    this.name = name;
    this.address = address;
    this.valid = true;
  }

  void handleUpdate(JsonObject update) {
    String type = update.getString("type");
    switch (type) {
      case "ADDED":
      case "MODIFIED":
      case "DELETED":
        JsonObject object = update.getJsonObject("object");
        JsonObject metadata = object.getJsonObject("metadata");
        String resourceVersion = metadata.getString("resourceVersion");
        updateEndpoints(object);
        break;
    }
  }

  void updateEndpoints(JsonObject item) {
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
          // Addresses array can be null when service pods are getting ready slowly
          // and are first added to notReadyAddresses array.
          if (addresses != null) {
            for (int k = 0;k < addresses.size();k++) {
              JsonObject address = addresses.getJsonObject(k);
              String ip = address.getString("ip");
              podIps.add(ip);
            }
          }
          for (int k = 0;k < ports.size();k++) {
            JsonObject port = ports.getJsonObject(k);
            int portNumber = port.getInteger("port");
            String portName = port.getString("name");
            if (address instanceof KubernetesServiceAddress) {
              KubernetesServiceAddress kubernetesAddress = (KubernetesServiceAddress) address;
              if (kubernetesAddress.portNumber > 0 && kubernetesAddress.portNumber != portNumber) {
                continue;
              }
              if (kubernetesAddress.portName != null && !kubernetesAddress.portName.equals(portName)) {
                continue;
              }
            }
            for (String podIp : podIps) {
              SocketAddress podAddress = SocketAddress.inetSocketAddress(portNumber, podIp);
              builder = builder.addServer(podAddress, podIp + "-" + port);
            }
            break;
          }
        }
      }
      this.endpoints.set(builder.build());
    }
  }
}
