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
package io.vertx.serviceresolver.srv.impl;

import io.vertx.core.dns.SrvRecord;
import io.vertx.core.spi.resolver.address.Endpoint;

import java.util.List;

class SrvServiceState {

  final long timestamp;
  final List<Endpoint<SrvRecord>> endpoints;

  public SrvServiceState(long timestamp, List<Endpoint<SrvRecord>> endpoints) {
    this.timestamp = timestamp;
    this.endpoints = endpoints;
  }

  boolean isValid() {
    long now = System.currentTimeMillis();
    for (Endpoint<SrvRecord> endpoint : endpoints) {
      if (now > endpoint.get().ttl() * 1000 + timestamp) {
        return false;
      }
    }
    return true;
  }
}
