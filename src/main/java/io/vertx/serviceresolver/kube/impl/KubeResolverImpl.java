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

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.*;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.Address;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.spi.resolver.address.AddressResolver;
import io.vertx.core.spi.resolver.address.Endpoint;
import io.vertx.serviceresolver.ServiceAddress;
import io.vertx.serviceresolver.kube.KubeResolverOptions;

import java.util.List;
import java.util.function.Function;

import static io.vertx.core.http.HttpMethod.GET;

public class KubeResolverImpl implements AddressResolver<ServiceAddress, SocketAddress, KubeServiceState> {

  final KubeResolverOptions options;
  final String host;
  final int port;
  Vertx vertx;
  WebSocketClient wsClient;
  HttpClient httpClient;
  final String namespace;
  final String bearerToken;

  public KubeResolverImpl(Vertx vertx, KubeResolverOptions options) {

    HttpClientOptions httpClientOptions = options.getHttpClientOptions();
    WebSocketClientOptions wsClientOptions = options.getWebSocketClientOptions();

    this.vertx = vertx;
    this.wsClient = vertx.createWebSocketClient(wsClientOptions == null ? new WebSocketClientOptions() : wsClientOptions);
    this.httpClient = vertx.createHttpClient(httpClientOptions == null ? new HttpClientOptions() : httpClientOptions);
    this.options = options;
    this.namespace = options.getNamespace();
    this.host = options.getHost();
    this.port = options.getPort();
    this.bearerToken = options.getBearerToken();
  }

  @Override
  public ServiceAddress tryCast(Address address) {
    return address instanceof ServiceAddress ? (ServiceAddress) address : null;
  }

  @Override
  public Future<KubeServiceState> resolve(Function<SocketAddress, Endpoint<SocketAddress>> factory, ServiceAddress address) {
    return httpClient
      .request(GET, port, host, "/api/v1/namespaces/" + namespace + "/endpoints")
      .compose(req -> {
        if (bearerToken != null) {
          req.putHeader(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken); // Todo concat that ?
        }
        return req.send().compose(resp -> {
          if (resp.statusCode() == 200) {
            return resp
              .body()
              .map(Buffer::toJsonObject);
          } else {
            return resp.body().transform(ar -> {
              StringBuilder msg = new StringBuilder("Invalid status code " + resp.statusCode());
              if (ar.succeeded()) {
                msg.append(" : ").append(ar.result().toString());
              }
              return Future.failedFuture(msg.toString());
            });
          }
        });
      }).map(response -> {
        String resourceVersion = response.getJsonObject("metadata").getString("resourceVersion");
        KubeServiceState state = new KubeServiceState(factory, this, vertx, resourceVersion, address.name());
        JsonArray items = response.getJsonArray("items");
        for (int i = 0;i < items.size();i++) {
          JsonObject item = items.getJsonObject(i);
          state.handleEndpoints(item);
        }
        return state;
      }).andThen(ar -> {
        if (ar.succeeded()) {
          KubeServiceState res = ar.result();
          res.connectWebSocket();
        }
      });
  }

  @Override
  public List<Endpoint<SocketAddress>> endpoints(KubeServiceState state) {
    return state.endpoints.get();
  }

  @Override
  public void close() {

  }

  @Override
  public SocketAddress addressOfEndpoint(SocketAddress endpoint) {
    return endpoint;
  }

  @Override
  public void dispose(KubeServiceState unused) {
    unused.disposed = true;
    if (unused.ws != null) {
      unused.ws.close();
    }
  }

  @Override
  public boolean isValid(KubeServiceState state) {
    return true;
  }
}
