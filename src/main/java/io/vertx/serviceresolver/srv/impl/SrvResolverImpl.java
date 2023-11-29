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
  public Future<SrvServiceState<B>> resolve(Function<SrvRecord, B> factory, ServiceAddress address) {
    Future<List<SrvRecord>> fut = client.resolveSRV(address.name());
    return fut.map(records -> {
      long ttl = 10_000_000;
      List<B> endpoints = new ArrayList<>();
      for (SrvRecord record : records) {
        endpoints.add(factory.apply(record));
        ttl = Math.min(ttl, record.ttl());
      }
      return new SrvServiceState<>(endpoints, System.currentTimeMillis() + 1000 * ttl);
    });
  }

  @Override
  public List<B> endpoints(SrvServiceState<B> state) {
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
