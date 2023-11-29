package io.vertx.serviceresolver.impl;

import io.vertx.core.Vertx;
import io.vertx.core.spi.resolver.address.AddressResolver;
import io.vertx.serviceresolver.ServiceResolver;

import java.util.function.BiFunction;

public class ServiceResolverImpl implements ServiceResolver {

  private final BiFunction<Vertx, ServiceResolverImpl, AddressResolver<?, ?, ?, ?>> provider;

  public ServiceResolverImpl(BiFunction<Vertx, ServiceResolverImpl, AddressResolver<?, ?, ?, ?>> provider) {
    this.provider = provider;
  }

  @Override
  public AddressResolver<?, ?, ?, ?> resolver(Vertx vertx) {
    return provider.apply(vertx, this);
  }
}
