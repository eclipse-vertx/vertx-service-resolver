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
import io.vertx.serviceresolver.impl.ServiceState;
import io.vertx.serviceresolver.loadbalancing.LoadBalancer;

import java.net.InetSocketAddress;
import java.util.List;

class DnsServiceState extends ServiceState<SocketAddress> {

  final long timestamp;


  DnsServiceState(SocketAddress address, long timestamp, List<InetSocketAddress> addresses, LoadBalancer loadBalancer) {
    super(address.hostName(), loadBalancer);

    for (InetSocketAddress addr : addresses) {
      add(SocketAddress.inetSocketAddress(address.port(), addr.getAddress().getHostAddress()));
    }

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
