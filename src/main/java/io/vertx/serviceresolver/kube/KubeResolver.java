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
package io.vertx.serviceresolver.kube;

import io.vertx.core.net.AddressResolver;
import io.vertx.serviceresolver.impl.ServiceAddressResolver;
import io.vertx.serviceresolver.kube.impl.KubeResolverImpl;

/**
 * A resolver for services within a Kubernetes cluster.
 */
public interface KubeResolver {

  /**
   * Create a Kubernetes resolver with the default options.
   *
   * @return the resolver
   */
  static AddressResolver create() {
    return create(new KubeResolverOptions());
  }

  /**
   * Create a Kubernetes resolver with the given {@code options}.
   *
   * @return the resolver
   */
  static AddressResolver create(KubeResolverOptions options) {
    return new ServiceAddressResolver((vertx, lookup) -> new KubeResolverImpl(vertx, options));
  }
}
