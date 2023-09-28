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
import io.vertx.core.dns.DnsClient;
import io.vertx.core.dns.SrvRecord;
import io.vertx.core.impl.VertxInternal;
import io.vertx.core.net.Address;
import io.vertx.core.net.SocketAddress;
import io.vertx.serviceresolver.ServiceAddress;
import io.vertx.serviceresolver.impl.ResolverBase;
import io.vertx.serviceresolver.srv.SrvLookup;

import java.util.List;

public class SrvResolverImpl extends ResolverBase<SrvServiceState> implements SrvLookup {

  final String host;
  final int port;
  final DnsClient client;

  public SrvResolverImpl(VertxInternal vertx, String host, int port) {
    super(vertx);
    this.host = host;
    this.port = port;
    this.client = vertx.createDnsClient(port, host);
  }

  @Override
  public ServiceAddress tryCast(Address address) {
    return address instanceof ServiceAddress ? (ServiceAddress) address : null;
  }

  @Override
  public Future<SrvServiceState> resolve(ServiceAddress address) {
    Future<List<SrvRecord>> fut = client.resolveSRV(address.name());
    return fut.map(records -> {
      SrvServiceState state = new SrvServiceState(address.name(), System.currentTimeMillis(), loadBalancer);
      state.add(records);
      return state;
    });
  }

  @Override
  public void removeAddress(SrvServiceState state, SocketAddress address) {
    // TODO
  }

  @Override
  public void dispose(SrvServiceState state) {
    // TODO
  }
}
