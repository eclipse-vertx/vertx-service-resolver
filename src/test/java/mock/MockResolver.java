package mock;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.net.Address;
import io.vertx.core.net.SocketAddress;
import io.vertx.serviceresolver.impl.*;
import io.vertx.serviceresolver.ServiceAddress;
import io.vertx.serviceresolver.ServiceResolver;
import io.vertx.serviceresolver.loadbalancing.LoadBalancer;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class MockResolver implements ResolverPlugin<ServiceAddress, SocketAddress, MockServiceState> {

  public static ServiceResolver create(MockController controller) {
    return new ServiceResolverImpl((vertx, lookup) -> {
      MockResolver resolver = new MockResolver();
      controller.resolver = resolver;
//      controller.deferred.forEach(reg -> resolver.register(reg.name, reg.endpoints));
      return new ResolverImpl<>(vertx, lookup.loadBalancer, resolver);
    });
  }

  private final ConcurrentMap<String, List<SocketAddress>> templates;
  private final ConcurrentMap<String, MockServiceState> entries;

  public MockResolver() {
    this.templates = new ConcurrentHashMap<>();
    this.entries = new ConcurrentHashMap<>();
  }

  public void register(String name, List<SocketAddress> endpoints) {
    templates.put(name, endpoints);
  }

  @Override
  public void init(Vertx vertx) {

  }

  @Override
  public ServiceAddress tryCast(Address address) {
    return address instanceof ServiceAddress ? (ServiceAddress) address : null;
  }

  @Override
  public SocketAddress addressOfEndpoint(SocketAddress endpoint) {
    return endpoint;
  }

  @Override
  public Future<MockServiceState> resolve(LoadBalancer loadBalancer, ServiceAddress address) {
    List<SocketAddress> endpoints = templates.get(address.name());
    if (endpoints == null) {
      return Future.failedFuture("No addresses for service svc");
    }
    MockServiceState state = new MockServiceState(loadBalancer.selector());
    state.set(endpoints);
    return Future.succeededFuture(state);
  }

  @Override
  public void dispose(MockServiceState state) {
    state.disposed = true;
  }
}
