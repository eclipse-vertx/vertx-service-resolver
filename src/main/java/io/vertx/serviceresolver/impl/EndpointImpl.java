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

import io.vertx.serviceresolver.Endpoint;

final class EndpointImpl<E> implements Endpoint<E> {

  final ServiceState<E> state;
  final E value;

  public EndpointImpl(ServiceState<E> state, E value) {
    this.state = state;
    this.value = value;
  }

  @Override
  public E get() {
    return value;
  }

  void reportRequestMetric(long latency) {
    // TODO
  }

  void reportResponseMetric(long latency) {
    // TODO
  }

}
