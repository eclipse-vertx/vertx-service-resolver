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

import io.vertx.core.Future;
import io.vertx.core.dns.SrvRecord;
import io.vertx.core.spi.endpoint.EndpointBuilder;
import io.vertx.serviceresolver.ServiceAddress;

import java.util.List;

class SrvServiceState<B> {

  final ServiceAddress address;
  final SrvResolverImpl<B> resolver;
  final EndpointBuilder<B, SrvRecord> builder;
  private B endpoints;
  private long timerID;
  private boolean disposed;
  private boolean valid;

  public SrvServiceState(SrvResolverImpl<B> resolve, EndpointBuilder<B, SrvRecord> builder, ServiceAddress address) {
    this.resolver = resolve;
    this.address = address;
    this.builder = builder;
    this.timerID = -1L;
    this.valid = true;
  }

  synchronized B endpoints() {
    return endpoints;
  }

  synchronized boolean isValid() {
    return valid;
  }

  Future<?> refresh() {
    synchronized (this) {
      if (disposed || timerID >= 0L) {
        return null;
      }
      Future<List<SrvRecord>> fut = resolver.client.resolveSRV(address.name());
      return fut.andThen(ar -> {
        if (ar.succeeded()) {
          List<SrvRecord> records = ar.result();
          long ttl = Long.MAX_VALUE;
          EndpointBuilder<B, SrvRecord> tmp = builder;
          for (SrvRecord record : records) {
            tmp = tmp.addServer(record, record.target() + "-" + record.port());
            ttl = Math.min(ttl, record.ttl());
          }
          synchronized (SrvServiceState.this) {
            endpoints = tmp.build();
          }
          ttl = Math.max(ttl, resolver.minTTL);
          if (ttl > 0) {
            timerID = resolver.vertx.setTimer(ttl * 1000, id -> {
              synchronized (SrvServiceState.this) {
                timerID = -1;
              }
              refresh();
            });
          } else {
            valid = false;
          }
        }
      });
    }
  }

  void dispose() {
    long id;
    synchronized (this) {
      id = timerID;
      timerID = -1L;
      disposed = true;
    }
    if (id >= 0) {
      resolver.vertx.cancelTimer(id);
    }
  }
}
