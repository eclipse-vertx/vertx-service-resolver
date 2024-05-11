package mock;

import io.vertx.core.Future;
import io.vertx.core.net.Address;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.spi.resolver.address.AddressResolver;
import io.vertx.core.spi.resolver.address.EndpointListBuilder;
import io.vertx.serviceresolver.impl.*;
import io.vertx.serviceresolver.ServiceAddress;
import io.vertx.serviceresolver.ServiceResolver;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

public class MockResolver<B> implements AddressResolver<ServiceAddress, SocketAddress, MockServiceState<B>, B> {

  public static ServiceResolver create(MockController controller) {
    return new ServiceResolverImpl((vertx, lookup) -> {
      MockResolver resolver = new MockResolver();
      controller.resolver = resolver;
//      controller.deferred.forEach(reg -> resolver.register(reg.name, reg.endpoints));
      return resolver;
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
  public ServiceAddress tryCast(Address address) {
    return address instanceof ServiceAddress ? (ServiceAddress) address : null;
  }

  @Override
  public SocketAddress addressOfEndpoint(SocketAddress endpoint) {
    return endpoint;
  }

  @Override
  public Future<MockServiceState<B>> resolve(ServiceAddress address, EndpointListBuilder<B, SocketAddress> builder) {
    List<SocketAddress> endpoints = templates.get(address.name());
    if (endpoints == null) {
      return Future.failedFuture("No addresses for service svc");
    }
    MockServiceState<B> state = new MockServiceState<>();
//    state.set(endpoints);
    return Future.succeededFuture(state);
  }

  @Override
  public void dispose(MockServiceState state) {
    state.disposed = true;
  }

  @Override
  public List<B> endpoints(MockServiceState state) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isValid(MockServiceState state) {
    return true;
  }

  @Override
  public void close() {

  }
}
