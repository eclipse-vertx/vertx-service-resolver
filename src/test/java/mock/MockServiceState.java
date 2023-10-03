package mock;

import io.vertx.core.net.SocketAddress;
import io.vertx.serviceresolver.impl.ServiceState;
import io.vertx.serviceresolver.loadbalancing.EndpointSelector;
import io.vertx.serviceresolver.loadbalancing.LoadBalancer;

public class MockServiceState extends ServiceState<SocketAddress> {

  boolean disposed;
  volatile EndpointSelector selector;

  public MockServiceState(String name, LoadBalancer loadBalancer) {
    super(name, loadBalancer);
  }
}
