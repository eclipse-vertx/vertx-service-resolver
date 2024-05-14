package io.vertx.serviceresolver.impl;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.impl.VertxInternal;
import io.vertx.core.net.Address;
import io.vertx.core.net.endpoint.Endpoint;
import io.vertx.core.net.endpoint.impl.EndpointResolverInternal;
import io.vertx.serviceresolver.ServiceAddress;
import io.vertx.serviceresolver.ServiceResolverClient;

public class ServiceResolverClientImpl implements ServiceResolverClient {

  private final VertxInternal vertx;
  private final EndpointResolverInternal resolver;
  private long timerId;
  private boolean closed;

  public ServiceResolverClientImpl(Vertx vertx, EndpointResolverInternal resolver) {
    this.vertx = (VertxInternal) vertx;
    this.resolver = resolver;
  }

  public void init() {
    checkExpired();
  }

  private synchronized void checkExpired() {
    if (closed) {
      return;
    }
    timerId = vertx.setTimer(1000, id -> {
      synchronized (ServiceResolverClientImpl.this) {
        timerId = -1L;
      }
      resolver.checkExpired();
      checkExpired();
    });
  }

  @Override
  public Future<Endpoint> resolveEndpoint(Address address) {
    return resolver.resolveEndpoint(address);
  }

  @Override
  public Future<Endpoint> resolveEndpoint(ServiceAddress address) {
    return resolver.resolveEndpoint(address);
  }

  @Override
  public Future<Void> close() {
    long id;
    synchronized (this) {
      if (closed) {
        return vertx.getOrCreateContext().succeededFuture();
      }
      id = timerId;
      closed = true;
      timerId = -1L;
    }
    if (id >= 0) {
      vertx.cancelTimer(id);
    }
    return vertx.getOrCreateContext().succeededFuture();
  }
}
