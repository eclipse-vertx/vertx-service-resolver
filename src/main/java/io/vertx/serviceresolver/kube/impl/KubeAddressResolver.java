/*
 * Copyright (c) 2011-2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package io.vertx.serviceresolver.kube.impl;

import io.vertx.core.Vertx;
import io.vertx.core.net.AddressResolver;
import io.vertx.core.spi.endpoint.EndpointResolver;
import io.vertx.serviceresolver.ServiceAddress;
import io.vertx.serviceresolver.kube.KubeResolver;
import io.vertx.serviceresolver.kube.KubeResolverOptions;

import java.util.function.Supplier;

public class KubeAddressResolver implements KubeResolver {

  private final KubeResolverOptions options;
  private Supplier<String> tokenProvider;

  public KubeAddressResolver(KubeResolverOptions options) {
    this.options = options;
  }

  @Override
  public EndpointResolver<ServiceAddress, ?, ?, ?> endpointResolver(Vertx vertx) {
    Supplier<String> tokenProvider = this.tokenProvider;
    if (tokenProvider == null) {
      String token = options.getBearerToken();
      if (token != null) {
        if (token.equals(KubeResolverOptions.DEFAULT_TOKEN)) {
          // Special case
          tokenProvider = () -> KubeResolverImpl.defaultToken().orElse(null);
        } else {
          tokenProvider = () -> token;
        }
      } else {
        tokenProvider = null;
      }
    }
    return new KubeResolverImpl<>(vertx, tokenProvider, options);
  }

  @Override
  public KubeResolver tokenProvider(Supplier<String> tokenProvider) {
    this.tokenProvider = tokenProvider;
    return this;
  }
}
