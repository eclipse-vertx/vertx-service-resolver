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
import io.vertx.serviceresolver.loadbalancing.Endpoint;
import io.vertx.serviceresolver.loadbalancing.LoadBalancer;

public final class ResolverImpl<A extends Address, E, S extends ResolverState<E>> implements AddressResolver<S, A, RequestMetric<E>, EndpointImpl<E>> {

  private final Vertx vertx;
  private final LoadBalancer loadBalancer;
  private final ResolverPlugin<A, E, S> plugin;

  public ResolverImpl(Vertx vertx, LoadBalancer loadBalancer, ResolverPlugin<A, E, S> plugin) {

    if (loadBalancer == null) {
      loadBalancer = LoadBalancer.ROUND_ROBIN;
    }

    this.vertx = vertx;
    this.loadBalancer = loadBalancer;
    this.plugin = plugin;

    plugin.init(vertx);
  }

  @Override
  public A tryCast(Address address) {
    return plugin.tryCast(address);
  }

  @Override
  public Future<S> resolve(A address) {
    return plugin.resolve(loadBalancer, address);
  }

  @Override
  public void dispose(S state) {
    plugin.dispose(state);
  }

  @Override
  public EndpointImpl<E> pickEndpoint(S state) {
    return (EndpointImpl<E>) state.pickAddress();
  }

  @Override
  public boolean isValid(S state) {
    return state.isValid();
  }

  @Override
  public SocketAddress addressOf(EndpointImpl<E> endpoint) {
    return plugin.addressOfEndpoint(endpoint.get());
  }

  @Override
  public void removeAddress(S state, EndpointImpl<E> endpoint) {
    removeAddress(state, (Endpoint) endpoint);
  }

  public void removeAddress(S state, Endpoint endpoint) {

  }

  @Override
  public RequestMetric<E> initiateRequest(EndpointImpl<E> endpoint) {
    RequestMetric<E> metric = new RequestMetric<>(endpoint);
    endpoint.numberOfRequests.increment();
    endpoint.numberOfInflightRequests.increment();
    return metric;
  }

  @Override
  public void requestBegin(RequestMetric<E> metric) {
    metric.requestBegin = System.currentTimeMillis();
  }

  @Override
  public void requestEnd(RequestMetric<E> metric) {
    metric.requestEnd = System.currentTimeMillis();
  }

  @Override
  public void responseBegin(RequestMetric<E> metric) {
    metric.responseBegin = System.currentTimeMillis();
  }

  @Override
  public void responseEnd(RequestMetric<E> metric) {
    metric.responseEnd = System.currentTimeMillis();
    if (metric.failure == null) {
      metric.endpoint.reportRequestMetric(metric);
      metric.endpoint.numberOfInflightRequests.decrement();
    }
  }

  @Override
  public void requestFailed(RequestMetric<E> metric, Throwable failure) {
    if (metric.failure == null) {
      metric.failure = failure;
      metric.endpoint.numberOfInflightRequests.decrement();
      metric.endpoint.reportRequestFailure(failure);
    }
  }
}
