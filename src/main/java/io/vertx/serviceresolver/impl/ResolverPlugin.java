package io.vertx.serviceresolver.impl;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.net.Address;
import io.vertx.core.net.SocketAddress;
import io.vertx.serviceresolver.loadbalancing.LoadBalancer;

public interface ResolverPlugin<A extends Address, E, S extends ResolverState<E>> {

  void init(Vertx vertx);

  SocketAddress addressOfEndpoint(E endpoint);

  A tryCast(Address address);

  Future<S> resolve(LoadBalancer loadBalancer, A address);

  void dispose(S state);

}
