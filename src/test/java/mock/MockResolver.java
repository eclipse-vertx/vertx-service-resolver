package mock;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.net.SocketAddress;
import io.vertx.serviceresolver.Endpoint;
import io.vertx.serviceresolver.ServiceAddress;
import io.vertx.serviceresolver.ServiceResolver;
import io.vertx.serviceresolver.impl.ResolverBase;
import io.vertx.serviceresolver.impl.ServiceResolverImpl;
import io.vertx.serviceresolver.loadbalancing.LoadBalancer;

public class MockResolver extends ResolverBase<SocketAddress, MockServiceState> {

  private final MockController controller;

  public static ServiceResolver create(MockController controller) {
    return new ServiceResolverImpl((vertx, lookup) -> new MockResolver(vertx, lookup.loadBalancer, controller));
  }

  public MockResolver(Vertx vertx, LoadBalancer loadBalancer, MockController controller) {
    super(vertx, loadBalancer);

    this.controller = controller;
  }

  @Override
  public SocketAddress addressOf(Endpoint<SocketAddress> endpoint) {
    return endpoint.get();
  }

  @Override
  public Future<MockServiceState> resolve(ServiceAddress address) {
    MockServiceState state = controller.entries.get(address.name());
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
