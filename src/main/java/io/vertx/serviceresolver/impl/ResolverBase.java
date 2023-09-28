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
import io.vertx.core.Vertx;
import io.vertx.core.net.Address;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.spi.net.AddressResolver;
import io.vertx.serviceresolver.ServiceAddress;
import io.vertx.serviceresolver.loadbalancing.LoadBalancer;

public abstract class ResolverBase<T extends ServiceState<?>> implements AddressResolver<T, ServiceAddress, Void> {

  protected final Vertx vertx;
  protected final LoadBalancer loadBalancer;

  public ResolverBase(Vertx vertx, LoadBalancer loadBalancer) {

    if (loadBalancer == null) {
      loadBalancer = LoadBalancer.ROUND_ROBIN;
    }

    this.vertx = vertx;
    this.loadBalancer = loadBalancer;
  }

  @Override
  public ServiceAddress tryCast(Address address) {
    return address instanceof ServiceAddress ? (ServiceAddress) address : null;
  }

  @Override
  public Future<SocketAddress> pickAddress(T unused) {
    return unused.pickAddress();
  }

  @Override
  public boolean isValid(T state) {
    return state.isValid();
  }
}
