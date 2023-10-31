package mock;

import io.vertx.core.net.SocketAddress;
import io.vertx.serviceresolver.impl.ResolverState;
import io.vertx.serviceresolver.loadbalancing.EndpointSelector;

public class MockServiceState extends ResolverState<SocketAddress> {

  boolean disposed;

  public MockServiceState(EndpointSelector endpointSelector) {
    super(endpointSelector);
  }
}
