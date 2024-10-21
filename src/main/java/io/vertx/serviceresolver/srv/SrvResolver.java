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
package io.vertx.serviceresolver.srv;

import io.vertx.core.net.AddressResolver;
import io.vertx.serviceresolver.impl.ServiceAddressResolver;
import io.vertx.serviceresolver.srv.impl.SrvResolverImpl;

/**
 * DNS Srv resolver.
 */
public interface SrvResolver {

  /**
   * Create an address resolver configured by the {@code options}.
   *
   * @param options the resolver options
   * @return an address resolver
   */
  static AddressResolver create(SrvResolverOptions options) {
    return new ServiceAddressResolver((vertx, lookup) -> new SrvResolverImpl(vertx, options));
  }
}
