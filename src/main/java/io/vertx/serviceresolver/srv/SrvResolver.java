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

import io.vertx.serviceresolver.ServiceResolver;
import io.vertx.serviceresolver.impl.ServiceResolverImpl;
import io.vertx.serviceresolver.srv.impl.SrvResolverImpl;

/**
 * DNS Srv lookup.
 */
public interface SrvResolver {

  static ServiceResolver create(SrvResolverOptions options) {
    return new ServiceResolverImpl((vertx, lookup) -> new SrvResolverImpl(vertx, options));
  }
}
