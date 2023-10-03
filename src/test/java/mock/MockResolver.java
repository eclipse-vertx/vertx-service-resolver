package mock;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.net.SocketAddress;
import io.vertx.serviceresolver.loadbalancing.Endpoint;
import io.vertx.serviceresolver.ServiceAddress;
import io.vertx.serviceresolver.ServiceResolver;
import io.vertx.serviceresolver.impl.ResolverBase;
import io.vertx.serviceresolver.impl.ServiceResolverImpl;
import io.vertx.serviceresolver.loadbalancing.LoadBalancer;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class MockResolver extends ResolverBase<SocketAddress, MockServiceState> {

  public static ServiceResolver create(MockController controller) {
    return new ServiceResolverImpl((vertx, lookup) -> {
      MockResolver resolver = new MockResolver(vertx, lookup.loadBalancer);
      controller.resolver = resolver;
      controller.deferred.forEach(reg -> resolver.register(reg.name, reg.endpoints));
      return resolver;
    });
  }

  private final ConcurrentMap<String, MockServiceState> entries;

  public MockResolver(Vertx vertx, LoadBalancer loadBalancer) {
    super(vertx, loadBalancer);
    this.entries = new ConcurrentHashMap<>();
  }

  public void register(String name, List<SocketAddress> endpoints) {
    MockServiceState state = new MockServiceState(name, loadBalancer);
    endpoints.forEach(state::add);
    entries.put(name, state);
  }

  @Override
  public SocketAddress addressOf(Endpoint<SocketAddress> endpoint) {
    return endpoint.get();
  }

  @Override
  public Future<MockServiceState> resolve(ServiceAddress address) {
    MockServiceState state = entries.get(address.name());
    if (state == null) {
      return Future.failedFuture("No addresses for service svc");
    }
    if (state.selector == null) {
      state.selector = loadBalancer.selector();
    }
    return Future.succeededFuture(state);
  }

  @Override
  public void dispose(MockServiceState state) {
    state.disposed = true;
  }
}
