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

import io.vertx.serviceresolver.ServiceLookup;
import io.vertx.serviceresolver.impl.ServiceLookupImpl;
import io.vertx.serviceresolver.kube.impl.KubeResolverImpl;

/**
 * Kubernetes lookup.
 */
public interface KubeLookup {

  static ServiceLookup create(KubeLookupOptions options) {
    return new ServiceLookupImpl((vertx, lookup) -> new KubeResolverImpl(vertx, options.getNamespace(), options.getHost(), options.getPort(), options.getBearerToken(), options.getHttpClientOptions(), options.getWebSocketClientOptions()));
  }

}
