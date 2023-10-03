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

import io.vertx.serviceresolver.loadbalancing.Endpoint;
import io.vertx.serviceresolver.loadbalancing.EndpointSelector;
import io.vertx.serviceresolver.loadbalancing.LoadBalancer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public abstract class ServiceState<E> {

  public final String name;
  private final AtomicReference<List<Endpoint<E>>> endpoints = new AtomicReference<>(Collections.emptyList());
  private final EndpointSelector selector;

  public ServiceState(String name, LoadBalancer loadBalancer) {
    this.name = name;
    this.selector = loadBalancer.selector();
  }

  Endpoint<E> pickAddress() {
    List<Endpoint<E>> list = endpoints.get();
    if (list == null || list.isEmpty()) {
      return null;
    } else {
      return selector.selectEndpoint(list);
    }
  }

  public final void add(E endpoint) {
    while (true) {
      List<Endpoint<E>> list = endpoints.get();
      Endpoint<E> e = new EndpointImpl<>(this, endpoint);
      List<Endpoint<E>> copy;
      if (list.isEmpty()) {
        copy = Collections.singletonList(e);
      } else {
        copy = new ArrayList<>(list);
        copy.add(e);
      }
      if (endpoints.compareAndSet(list, copy)) {
        break;
      }
    }
  }

  public void clearEndpoints() {
    endpoints.set(Collections.emptyList());
  }

  public List<Endpoint<E>> endpoints() {
    return endpoints.get();
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
