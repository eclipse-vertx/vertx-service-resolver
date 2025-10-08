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
import io.vertx.serviceresolver.ServiceAddress;
import io.vertx.serviceresolver.kube.impl.KubeAddressResolver;

import java.util.function.Supplier;

/**
 * A resolver for services within a Kubernetes cluster.
 */
public interface KubeResolver extends AddressResolver<ServiceAddress> {

  /**
   * Create a Kubernetes resolver with the default options.
   *
   * @return the resolver
   */
  static KubeResolver create() {
    return create(new KubeResolverOptions());
  }

  /**
   * Create a Kubernetes resolver with the given {@code options}.
   *
   * @return the resolver
   */
  static KubeResolver create(KubeResolverOptions options) {
    return new KubeAddressResolver(options);
  }

  /**
   * Set a token provider for the resolver: the {@code tokenProvider} supplier is called when the resolver
   * needs a token or retries when the server responses with a {@code 401} code.
   *
   * @param tokenProvider the token provider called when a bearer token is needed
   * @return this instance
   */
  KubeResolver tokenProvider(Supplier<String> tokenProvider);

}
