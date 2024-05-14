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
package io.vertx.serviceresolver.srv.impl;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.dns.DnsClient;
import io.vertx.core.dns.SrvRecord;
import io.vertx.core.net.Address;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.spi.endpoint.EndpointBuilder;
import io.vertx.core.spi.endpoint.EndpointResolver;
import io.vertx.serviceresolver.ServiceAddress;
import io.vertx.serviceresolver.srv.SrvResolverOptions;

import java.util.List;

public class SrvResolverImpl<B> implements EndpointResolver<ServiceAddress, SrvRecord, SrvServiceState<B>, B> {

  Vertx vertx;
  DnsClient client;
  final String host;
  final int port;

  public SrvResolverImpl(Vertx vertx, SrvResolverOptions options) {
    this.host = options.getHost();
    this.port = options.getPort();
    this.vertx = vertx;
    this.client = vertx.createDnsClient(port, host);
  }

  @Override
  public ServiceAddress tryCast(Address address) {
    return address instanceof ServiceAddress ? (ServiceAddress) address : null;
  }

  @Override
  public Future<SrvServiceState<B>> resolve(ServiceAddress address, EndpointBuilder<B, SrvRecord> builder) {
    SrvServiceState<B> state = new SrvServiceState<>(this, builder, address);
    return state
      .refresh()
      .map(state);
  }

  private void resolve2(ServiceAddress address, EndpointBuilder<B, SrvRecord> builder) {

  }

  @Override
  public B endpoint(SrvServiceState<B> data) {
    return data.endpoints();
  }

  @Override
  public SocketAddress addressOf(SrvRecord record) {
    return SocketAddress.inetSocketAddress(record.port(), record.target());
  }

  @Override
  public void dispose(SrvServiceState state) {
    state.dispose();
  }

  @Override
  public boolean isValid(SrvServiceState state) {
    return true;
  }

  @Override
  public void close() {
  }
}
