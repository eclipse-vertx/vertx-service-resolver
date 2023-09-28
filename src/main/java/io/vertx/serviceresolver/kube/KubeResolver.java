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

import io.vertx.core.Vertx;
import io.vertx.core.net.AddressResolver;
import io.vertx.serviceresolver.kube.impl.KubeResolverImpl;

public interface KubeResolver extends AddressResolver {

  static KubeResolver create(Vertx vertx, KubeResolverOptions options) {
    return new KubeResolverImpl(vertx, options.getNamespace(), options.getHost(), options.getPort(), options.getBearerToken(), options.getHttpClientOptions(), options.getWebSocketClientOptions());
  }

}
