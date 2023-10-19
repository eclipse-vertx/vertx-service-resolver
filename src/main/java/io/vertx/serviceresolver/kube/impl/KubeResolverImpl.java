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
import io.vertx.core.net.SocketAddress;
import io.vertx.serviceresolver.impl.EndpointImpl;
import io.vertx.serviceresolver.loadbalancing.Endpoint;
import io.vertx.serviceresolver.ServiceAddress;
import io.vertx.serviceresolver.impl.ResolverBase;
import io.vertx.serviceresolver.kube.KubeResolverOptions;
import io.vertx.serviceresolver.loadbalancing.LoadBalancer;

import static io.vertx.core.http.HttpMethod.GET;

public class KubeResolverImpl extends ResolverBase<SocketAddress, KubeServiceState> {

  final String host;
  final int port;
  final WebSocketClient wsClient;
  final HttpClient httpClient;
  final String namespace;
  final String bearerToken;

  public KubeResolverImpl(Vertx vertx,
                          LoadBalancer loadBalancer,
                          KubeResolverOptions options) {
    super(vertx, loadBalancer);

    HttpClientOptions httpClientOptions = options.getHttpClientOptions();
    WebSocketClientOptions wsClientOptions = options.getWebSocketClientOptions();

    this.namespace = options.getNamespace();
    this.host = options.getHost();
    this.port = options.getPort();
    this.bearerToken = options.getBearerToken();
    this.wsClient = vertx.createWebSocketClient(wsClientOptions == null ? new WebSocketClientOptions() : wsClientOptions);
    this.httpClient = vertx.createHttpClient(httpClientOptions == null ? new HttpClientOptions() : httpClientOptions);
  }

  @Override
  public Future<KubeServiceState> resolve(ServiceAddress serviceName) {
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
        KubeServiceState state = new KubeServiceState(this, vertx, resourceVersion, serviceName.name(), loadBalancer);
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
}
