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
import io.vertx.core.spi.endpoint.EndpointBuilder;
import io.vertx.core.spi.endpoint.EndpointResolver;
import io.vertx.serviceresolver.ServiceAddress;
import io.vertx.serviceresolver.kube.KubeResolverOptions;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;
import java.util.function.Supplier;

public class KubeResolverImpl<B> implements EndpointResolver<ServiceAddress, SocketAddress, KubeServiceState<B>, B> {

  public static final String KUBERNETES_SERVICE_HOST = "KUBERNETES_SERVICE_HOST";
  public static final String KUBERNETES_SERVICE_PORT = "KUBERNETES_SERVICE_PORT";
  public static final String KUBERNETES_SERVICE_ACCOUNT_TOKEN = "/var/run/secrets/kubernetes.io/serviceaccount/token";
  public static final String KUBERNETES_SERVICE_ACCOUNT_CA = "/var/run/secrets/kubernetes.io/serviceaccount/ca.crt";
  public static final String KUBERNETES_SERVICE_ACCOUNT_NAMESPACE = "/var/run/secrets/kubernetes.io/serviceaccount/namespace";

  public static Optional<String> defaultToken() {
    File tokenFile = new File(KUBERNETES_SERVICE_ACCOUNT_TOKEN);
    if (tokenFile.exists()) {
      try {
        String token = Buffer.buffer(Files.readAllBytes(tokenFile.toPath())).toString();
        return Optional.of(token);
      } catch (IOException ignore) {
      }
    }
    return Optional.empty();
  }

  final KubeResolverOptions options;
  final Supplier<String> tokenProvider;
  final SocketAddress server;
  Vertx vertx;
  WebSocketClient wsClient;
  HttpClient httpClient;
  final String namespace;
  private String cachedToken;

  public KubeResolverImpl(Vertx vertx, Supplier<String> tokenProvider, KubeResolverOptions options) {

    HttpClientOptions httpClientOptions = options.getHttpClientOptions();
    WebSocketClientOptions wsClientOptions = options.getWebSocketClientOptions();

    this.vertx = vertx;
    this.wsClient = vertx.createWebSocketClient(wsClientOptions == null ? new WebSocketClientOptions() : wsClientOptions);
    this.httpClient = vertx.createHttpClient(httpClientOptions == null ? new HttpClientOptions() : httpClientOptions);
    this.options = options;
    this.namespace = options.getNamespace();
    this.server = options.getServer();
    this.tokenProvider = tokenProvider;
  }

  @Override
  public ServiceAddress tryCast(Address address) {
    return address instanceof ServiceAddress ? (ServiceAddress) address : null;
  }

  private synchronized String token() {
    String token = cachedToken;
    if (token == null) {
      if (tokenProvider != null) {
        token = tokenProvider.get();
        if (token != null) {
          cachedToken = token;
        }
      }
    }
    return token;
  }

  private synchronized void setToken(String token) {
    this.cachedToken = token;;
  }

  private static class EndpoinsRequest<T> {
    final T payload;
    final String token;
    final int retries;
    EndpoinsRequest(T payload, String token, int retries) {
      this.payload = payload;
      this.token = token;
      this.retries = retries;
    }
  }

  Future<EndpoinsRequest<JsonObject>> requestEndpoints(String token, int retries) {
    return httpClient
      .request(new RequestOptions()
        .setMethod(HttpMethod.GET)
        .setServer(server)
        .setURI("/api/v1/namespaces/" + namespace + "/endpoints"))
      .compose(req -> {
        if (token != null) {
          req.putHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        return req.send().compose(resp -> {
          if (resp.statusCode() == 200) {
            return resp.body().map(body -> new EndpoinsRequest<>(new JsonObject(body), token, retries));
          } else if (resp.statusCode() == 401) {
            // It could be an expired token
            if (tokenProvider != null && retries < 3) {
              String freshToken = tokenProvider.get();
              if (freshToken != null && !freshToken.equals(token)) {
                return requestEndpoints(freshToken, retries + 1);
              }
            }
          }
          return resp.body().transform(ar -> {
            StringBuilder msg = new StringBuilder("Invalid status code " + resp.statusCode());
            if (ar.succeeded()) {
              msg.append(" : ").append(ar.result().toString());
            }
            return Future.failedFuture(msg.toString());
          });
        });
      });
  }

  @Override
  public Future<KubeServiceState<B>> resolve(ServiceAddress address, EndpointBuilder<B, SocketAddress> builder) {
    String token = token();
    Future<EndpoinsRequest<JsonObject>> endpointsFuture = requestEndpoints(token, 0);
    return endpointsFuture
      .compose(endpointsRequest -> {
        KubeServiceState<B> state = new KubeServiceState<>(builder, address, address.name());
        JsonObject response = endpointsRequest.payload;
        String resourceVersion = response.getJsonObject("metadata").getString("resourceVersion");
        JsonArray items = response.getJsonArray("items");
        for (int i = 0; i < items.size(); i++) {
          JsonObject item = items.getJsonObject(i);
          state.updateEndpoints(item);
        }
        if (endpointsRequest.token != null && !endpointsRequest.token.equals(token)) {
          setToken(endpointsRequest.token);
        }
        return connectWebSocket(state, resourceVersion, endpointsRequest.token).map(state);
      });
  }

  private Future<WebSocket> connectWebSocket(KubeServiceState<B> state, String resourceVersion, String token) {
    String requestURI = "/api/v1/namespaces/" + namespace + "/endpoints?"
      + "watch=true"
      + "&"
      + "allowWatchBookmarks=true"
      + "&"
      + "resourceVersion=" + resourceVersion;
    WebSocketConnectOptions connectOptions = new WebSocketConnectOptions();
    connectOptions.setServer(server);
    connectOptions.setURI(requestURI);
    if (token != null) {
      connectOptions.putHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }
    return wsClient.webSocket()
      .handler(buff -> {
        JsonObject update  = buff.toJsonObject();
        state.handleUpdate(update);
      })
      .closeHandler(v -> {
        state.valid = false;
      }).connect(connectOptions);
  }

  @Override
  public B endpoint(KubeServiceState<B> data) {
    return data.endpoints.get();
  }

  @Override
  public void close() {
    httpClient.close();
    wsClient.close();
  }

  @Override
  public SocketAddress addressOf(SocketAddress endpoint) {
    return endpoint;
  }

  @Override
  public void dispose(KubeServiceState<B> unused) {
    unused.disposed = true;
    if (unused.webSocket != null) {
      unused.webSocket.close();
    }
  }

  @Override
  public boolean isValid(KubeServiceState<B> state) {
    return state.valid;
  }
}
