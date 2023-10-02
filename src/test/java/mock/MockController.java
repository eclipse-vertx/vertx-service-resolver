package mock;

import io.vertx.core.net.SocketAddress;
import io.vertx.serviceresolver.loadbalancing.LoadBalancer;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class MockController {

  final ConcurrentMap<String, MockServiceState> entries = new ConcurrentHashMap<>();

  public void register(String name, List<SocketAddress> endpoints) {
    MockServiceState state = new MockServiceState(name, LoadBalancer.ROUND_ROBIN);
    endpoints.forEach(state::add);
    entries.put(name, state);
  }
}
