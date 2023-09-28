package io.vertx.serviceresolver.impl;

import io.vertx.core.Vertx;
import io.vertx.core.spi.lookup.AddressResolver;
import io.vertx.serviceresolver.ServiceLookup;
import io.vertx.serviceresolver.loadbalancing.LoadBalancer;

import java.util.function.BiFunction;
import java.util.function.Function;

public class ServiceLookupImpl implements ServiceLookup {

  private final BiFunction<Vertx, ServiceLookupImpl, AddressResolver<?, ?, ?>> provider;
  private LoadBalancer loadBalancer;

  public ServiceLookupImpl(BiFunction<Vertx, ServiceLookupImpl, AddressResolver<?, ?, ?>> provider) {
    this.provider = provider;
  }

  @Override
  public AddressResolver<?, ?, ?> resolver(Vertx vertx) {
    return provider.apply(vertx, this);
  }

  @Override
  public ServiceLookup withLoadBalancer(LoadBalancer loadBalancer) {
    return null;
  }
}
