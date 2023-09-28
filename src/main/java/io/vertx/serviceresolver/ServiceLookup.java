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
package io.vertx.serviceresolver;

import io.vertx.core.net.AddressLookup;
import io.vertx.serviceresolver.loadbalancing.LoadBalancer;

/**
 * Address lookup implementing service discovery and load balancing.
 */
public interface ServiceLookup extends AddressLookup {

  /**
   * Set the load balancer to use.
   * @param loadBalancer the load balancer
   * @return a reference to this, so the API can be used fluently
   */
  ServiceLookup withLoadBalancer(LoadBalancer loadBalancer);

}
