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
package io.vertx.serviceresolver.loadbalancing;

public interface Endpoint<V> {

  /**
   * @return the endpoint payload
   */
  V get();

  /**
   * @return the number of inflight requests
   */
  int numberOfInflightRequests();

  /**
   * @return the total number of requests
   */
  int numberOfRequests();

  /**
   * @return the total number of failures
   */
  int numberOfFailures();

  int minResponseTime();

  int maxResponseTime();

}
