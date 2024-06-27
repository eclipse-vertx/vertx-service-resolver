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

import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.internal.VertxInternal;
import io.vertx.core.net.AddressResolver;
import io.vertx.core.net.endpoint.Endpoint;
import io.vertx.core.net.endpoint.EndpointResolver;
import io.vertx.core.net.endpoint.LoadBalancer;
import io.vertx.core.net.endpoint.impl.EndpointResolverImpl;
import io.vertx.serviceresolver.impl.ServiceResolverClientImpl;
import io.vertx.serviceresolver.kube.KubeResolver;
import io.vertx.serviceresolver.kube.KubeResolverOptions;
import io.vertx.serviceresolver.srv.SrvResolver;
import io.vertx.serviceresolver.srv.SrvResolverOptions;

/**
 * Address lookup implementing service discovery and load balancing.
 */
@VertxGen
public interface ServiceResolverClient extends EndpointResolver {

  /**
   * Create a new service resolver client.
   *
   * @param vertx the vertx instance
   * @param options the resolver options
   * @return the service resolver client
   */
  static ServiceResolverClient create(Vertx vertx, ServiceResolverOptions options) {
    return create(vertx, LoadBalancer.ROUND_ROBIN, options);
  }

  /**
   * Create a new service resolver client.
   *
   * @param vertx the vertx instance
   * @param loadBalancer the load balancer
   * @param options the resolver options
   * @return the service resolver client
   */
  @GenIgnore(GenIgnore.PERMITTED_TYPE)
  static ServiceResolverClient create(Vertx vertx, LoadBalancer loadBalancer, ServiceResolverOptions options) {
    AddressResolver addressResolver;
    if (options instanceof KubeResolverOptions) {
      addressResolver = KubeResolver.create((KubeResolverOptions) options);
    } else if (options instanceof SrvResolverOptions) {
      addressResolver = SrvResolver.create((SrvResolverOptions) options);
    } else {
      throw new IllegalArgumentException();
    }
    io.vertx.core.spi.endpoint.EndpointResolver<ServiceAddress, ?, ?, ?> resolver = (io.vertx.core.spi.endpoint.EndpointResolver<ServiceAddress, ?, ?, ?>)addressResolver.endpointResolver(vertx);
    EndpointResolverImpl<?, ServiceAddress, ?> r = new EndpointResolverImpl<>((VertxInternal) vertx, resolver, loadBalancer, 1000);
    ServiceResolverClientImpl r2 = new ServiceResolverClientImpl(vertx, r);
    r2.init();
    return r2;
  }

  Future<Endpoint> resolveEndpoint(ServiceAddress address);

  /**
   * Close the client and release the resources.
   *
   * @return a future notified when the resources are disposed
   */
  Future<Void> close();

}
