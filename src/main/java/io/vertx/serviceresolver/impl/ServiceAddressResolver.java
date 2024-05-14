package io.vertx.serviceresolver.impl;

import io.vertx.core.Vertx;
import io.vertx.core.spi.endpoint.EndpointResolver;

import java.util.function.BiFunction;

public class ServiceAddressResolver implements io.vertx.core.net.AddressResolver {

  private final BiFunction<Vertx, ServiceAddressResolver, EndpointResolver<?, ?, ?, ?>> provider;

  public ServiceAddressResolver(BiFunction<Vertx, ServiceAddressResolver, EndpointResolver<?, ?, ?, ?>> provider) {
    this.provider = provider;
  }

  @Override
  public EndpointResolver<?, ?, ?, ?> endpointResolver(Vertx vertx) {
    return provider.apply(vertx, this);
  }
}
