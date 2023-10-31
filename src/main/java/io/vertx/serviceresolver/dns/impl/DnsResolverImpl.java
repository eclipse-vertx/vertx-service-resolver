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
package io.vertx.serviceresolver.dns.impl;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.dns.AddressResolverOptions;
import io.vertx.core.impl.AddressResolver;
import io.vertx.core.impl.VertxInternal;
import io.vertx.core.net.Address;
import io.vertx.core.net.SocketAddress;
import io.vertx.serviceresolver.ServiceAddress;
import io.vertx.serviceresolver.dns.DnsResolverOptions;
import io.vertx.serviceresolver.impl.ResolverBase;
import io.vertx.serviceresolver.loadbalancing.LoadBalancer;
import io.vertx.serviceresolver.srv.SrvResolver;

import java.util.Collections;

public class DnsResolverImpl extends ResolverBase<SocketAddress, SocketAddress, DnsServiceState> implements SrvResolver {

  private AddressResolver dnsResolver;

  public DnsResolverImpl(Vertx vertx, DnsResolverOptions options, LoadBalancer loadBalancer) {
    super(vertx, loadBalancer);

    AddressResolverOptions o = new AddressResolverOptions();
    o.setServers(Collections.singletonList(options.getHost() + ":" + options.getPort()));
    dnsResolver = new AddressResolver(vertx, o);
  }

  @Override
  public SocketAddress tryCast(Address address) {
    return address instanceof ServiceAddress ? (SocketAddress) address : null;
  }

  @Override
  public SocketAddress addressOfEndpoint(SocketAddress endpoint) {
    return endpoint;
  }

  @Override
  public Future<DnsServiceState> resolve(SocketAddress address) {
    Promise<DnsServiceState> promise = Promise.promise();
    dnsResolver.resolveHostnameAll(address.host(), ar -> {
      if (ar.succeeded()) {
        promise.complete(new DnsServiceState(address, 100, ar.result(), loadBalancer));
      } else {
        promise.fail(ar.cause());
      }
    });
    return promise.future();
  }

  @Override
  public void dispose(DnsServiceState state) {

  }
}
