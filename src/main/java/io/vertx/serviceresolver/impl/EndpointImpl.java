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

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

final class EndpointImpl<E> implements Endpoint<E> {

  final ServiceState<E> state;
  final E value;
  final LongAdder numberOfInflightRequests = new LongAdder();
  final LongAdder numberOfRequests = new LongAdder();
  final LongAdder numberOfFailures = new LongAdder();
  final AtomicLong minResponseTime = new AtomicLong(Long.MAX_VALUE);
  final AtomicLong maxResponseTime = new AtomicLong(0);

  public EndpointImpl(ServiceState<E> state, E value) {
    this.state = state;
    this.value = value;
  }

  @Override
  public E get() {
    return value;
  }

  public int numberOfInflightRequests() {
    return numberOfInflightRequests.intValue();
  }

  public int numberOfRequests() {
    return numberOfRequests.intValue();
  }

  @Override
  public int numberOfFailures() {
    return numberOfFailures.intValue();
  }

  @Override
  public int minResponseTime() {
    return minResponseTime.intValue();
  }

  @Override
  public int maxResponseTime() {
    return maxResponseTime.intValue();
  }

  void reportRequestMetric(RequestMetric<?> metric) {
    long responseTime = metric.responseEnd - metric.requestBegin;
    while (true) {
      long val = minResponseTime.get();
      if (responseTime >= val || minResponseTime.compareAndSet(val, responseTime)) {
        break;
      }
    }
    while (true) {
      long val = maxResponseTime.get();
      if (responseTime <= val || maxResponseTime.compareAndSet(val, responseTime)) {
        break;
      }
    }
  }

  void reportRequestFailure(Throwable failure) {
    numberOfFailures.increment();
  }
}
