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
import io.vertx.core.spi.resolver.address.AddressResolver;
import io.vertx.core.spi.resolver.address.EndpointListBuilder;
import io.vertx.serviceresolver.ServiceAddress;
import io.vertx.serviceresolver.srv.SrvResolverOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class SrvResolverImpl<B> implements AddressResolver<ServiceAddress, SrvRecord, SrvServiceState<B>, B> {

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
  public Future<SrvServiceState<B>> resolve(ServiceAddress address, EndpointListBuilder<B, SrvRecord> builder) {
    Future<List<SrvRecord>> fut = client.resolveSRV(address.name());
    return fut.map(records -> {
      long ttl = 10_000_000;
      EndpointListBuilder<B, SrvRecord> tmp = builder;
      for (SrvRecord record : records) {
        tmp = tmp.addEndpoint(record, record.target() + "-" + record.port());
        ttl = Math.min(ttl, record.ttl());
      }
      return new SrvServiceState<>(tmp.build(), System.currentTimeMillis() + 1000 * ttl);
    });
  }

  @Override
  public B endpoints(SrvServiceState<B> state) {
    return state.endpoints;
  }

  @Override
  public SocketAddress addressOfEndpoint(SrvRecord record) {
    return SocketAddress.inetSocketAddress(record.port(), record.target());
  }

  @Override
  public void dispose(SrvServiceState state) {
    // TODO
  }

  @Override
  public boolean isValid(SrvServiceState state) {
    return state.isValid();
  }

  @Override
  public void close() {
  }
}
