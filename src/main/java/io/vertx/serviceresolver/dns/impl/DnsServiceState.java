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

import io.vertx.core.net.SocketAddress;
import io.vertx.serviceresolver.impl.ResolverState;
import io.vertx.serviceresolver.loadbalancing.EndpointSelector;
import io.vertx.serviceresolver.loadbalancing.LoadBalancer;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

class DnsServiceState extends ResolverState<SocketAddress> {

  final long timestamp;


  DnsServiceState(SocketAddress address, long timestamp, List<InetSocketAddress> addresses, EndpointSelector endpointSelector) {
    super(endpointSelector);

    List<SocketAddress> endpoints = new ArrayList<>();
    for (InetSocketAddress addr : addresses) {
      endpoints.add(SocketAddress.inetSocketAddress(address.port(), addr.getAddress().getHostAddress()));
    }
    set(endpoints);

    this.timestamp = timestamp;
  }

  @Override
  protected boolean isValid() {
//    long now = System.currentTimeMillis();
//    for (EndpointImpl<SrvRecord> endpoint : endpoints()) {
//      if (now > endpoint.get().ttl() * 1000 + timestamp) {
//        return false;
//      }
//    }
    return true;
  }
}
