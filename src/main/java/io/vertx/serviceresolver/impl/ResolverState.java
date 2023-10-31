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

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public abstract class ResolverState<E> {

  private final AtomicReference<List<EndpointImpl<E>>> endpoints = new AtomicReference<>(Collections.emptyList());
  private final EndpointSelector selector;

  public ResolverState(EndpointSelector selector) {
    this.selector = selector;
  }

  Endpoint pickAddress() {
    List<EndpointImpl<E>> list = endpoints.get();
    if (list == null || list.isEmpty()) {
      return null;
    } else {
      return selector.selectEndpoint(list);
    }
  }

  protected boolean isValid() {
    return true;
  }

  public final void add(E endpoint) {
    while (true) {
      List<EndpointImpl<E>> list = endpoints.get();
      EndpointImpl<E> e = new EndpointImpl<>(endpoint);
      List<EndpointImpl<E>> copy;
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

  public final List<E> endpoints() {
    List<EndpointImpl<E>> list = endpoints.get();
    return new AbstractList<E>() {
      @Override
      public E get(int index) {
        EndpointImpl<E> e = list.get(index);
        return e != null ? e.get() : null;
      }
      @Override
      public int size() {
        return list.size();
      }
    };
  }

  public final void set(List<E> endpoints) {
    List<EndpointImpl<E>> list = new ArrayList<>();
    for (E e : endpoints) {
      list.add(new EndpointImpl<>(e));
    }
    this.endpoints.set(list);
  }
}
