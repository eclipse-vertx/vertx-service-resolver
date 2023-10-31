package mock;

import io.vertx.core.net.SocketAddress;

import java.util.List;

public class MockController {

  MockResolver resolver;

  public void register(String name, List<SocketAddress> endpoints) {
    resolver.register(name, endpoints);
  }
}
