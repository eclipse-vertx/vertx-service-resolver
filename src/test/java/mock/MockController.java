package mock;

import io.vertx.core.net.SocketAddress;

import java.util.ArrayList;
import java.util.List;

public class MockController {

  static class Registration {
    final String name;
    final List<SocketAddress> endpoints;
    Registration(String name, List<SocketAddress> endpoints) {
      this.name = name;
      this.endpoints = endpoints;
    }
  }

  MockResolver resolver;
  List<Registration> deferred = new ArrayList<>();

  public void register(String name, List<SocketAddress> endpoints) {
    if (resolver != null) {
      resolver.register(name, endpoints);
    } else {
      deferred.add(new Registration(name, endpoints));
    }
  }
}
