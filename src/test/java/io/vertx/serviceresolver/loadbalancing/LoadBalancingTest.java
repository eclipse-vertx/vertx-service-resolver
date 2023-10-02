package io.vertx.serviceresolver.loadbalancing;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.net.SocketAddress;
import io.vertx.ext.unit.TestContext;
import io.vertx.serviceresolver.ServiceAddress;
import io.vertx.serviceresolver.ServiceResolver;
import io.vertx.serviceresolver.ServiceResolverTestBase;
import mock.MockController;
import mock.MockResolver;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class LoadBalancingTest extends ServiceResolverTestBase {

  @Test
  public void testRoundRobin(TestContext ctx) throws Exception {
    List<SocketAddress> pods = startPods(3, req -> {
      req.response().end("" + req.localAddress().port());
    });
    MockController controller = new MockController();
    ServiceResolver resolver = MockResolver.create(controller).withLoadBalancer(LoadBalancer.ROUND_ROBIN);
    client = vertx
      .httpClientBuilder()
      .withAddressResolver(resolver)
      .build();
    controller.register("svc", Arrays.asList(SocketAddress.inetSocketAddress(8080, "localhost"), SocketAddress.inetSocketAddress(8081, "localhost"), SocketAddress.inetSocketAddress(8082, "localhost")));
    ctx.assertEquals(Buffer.buffer("8080"), get(ServiceAddress.create("svc")));
    ctx.assertEquals(Buffer.buffer("8081"), get(ServiceAddress.create("svc")));
    ctx.assertEquals(Buffer.buffer("8082"), get(ServiceAddress.create("svc")));
    ctx.assertEquals(Buffer.buffer("8080"), get(ServiceAddress.create("svc")));
    ctx.assertEquals(Buffer.buffer("8081"), get(ServiceAddress.create("svc")));
    ctx.assertEquals(Buffer.buffer("8082"), get(ServiceAddress.create("svc")));
  }

}
