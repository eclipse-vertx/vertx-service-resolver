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
import io.vertx.core.http.*;
import io.vertx.core.net.Address;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.spi.endpoint.EndpointBuilder;
import io.vertx.core.spi.endpoint.EndpointResolver;
import io.vertx.serviceresolver.ServiceAddress;
import io.vertx.serviceresolver.kube.KubeResolverOptions;

public class KubeResolverImpl<B> implements EndpointResolver<ServiceAddress, SocketAddress, KubeServiceState<B>, B> {

  final KubeResolverOptions options;
  final SocketAddress server;
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
    this.server = options.getServer();
    this.bearerToken = options.getBearerToken();
  }

  @Override
  public ServiceAddress tryCast(Address address) {
    return address instanceof ServiceAddress ? (ServiceAddress) address : null;
  }

  @Override
  public Future<KubeServiceState<B>> resolve(ServiceAddress address, EndpointBuilder<B, SocketAddress> builder) {
    KubeServiceState<B> state = new KubeServiceState<>(builder, this, address, vertx, address.name());
    return state
      .connect()
      .map(state);
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
    if (unused.ws != null) {
      unused.ws.close();
    }
  }

  @Override
  public boolean isValid(KubeServiceState<B> state) {
    return state.valid;
  }
}
