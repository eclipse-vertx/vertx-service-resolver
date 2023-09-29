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
package io.vertx.serviceresolver.impl;

import io.vertx.core.Future;
import io.vertx.core.net.SocketAddress;
import io.vertx.serviceresolver.Endpoint;
import io.vertx.serviceresolver.loadbalancing.EndpointSelector;
import io.vertx.serviceresolver.loadbalancing.LoadBalancer;

import java.util.ArrayList;
import java.util.List;

public abstract class ServiceState<E> {

  public final String name;
  public final List<EndpointImpl<E>> endpoints = new ArrayList<>();
  private final EndpointSelector selector;

  public ServiceState(String name, LoadBalancer loadBalancer) {
    this.name = name;
    this.selector = loadBalancer.selector();
  }

  Future<EndpointImpl<E>> pickAddress() {
    if (endpoints.isEmpty()) {
      return Future.failedFuture("No addresses for service " + name);
    } else {
      EndpointImpl<E> endpoint = selector.selectEndpoint(endpoints);
      return Future.succeededFuture(endpoint);
    }
  }

  public final void add(E endpoint) {
    endpoints.add(new EndpointImpl<>(this, endpoint));
  }

  public final void add(List<E> endpoints) {
    for (E endpoint : endpoints) {
      add(endpoint);
    }
  }

  protected boolean isValid() {
    return true;
  }


}
