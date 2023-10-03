package io.vertx.serviceresolver.loadbalancing;

import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.net.SocketAddress;
import io.vertx.ext.unit.TestContext;
import io.vertx.serviceresolver.ServiceAddress;
import io.vertx.serviceresolver.ServiceResolver;
import io.vertx.serviceresolver.ServiceResolverTestBase;
import mock.MockController;
import mock.MockResolver;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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
    controller.register("svc", pods);
    ctx.assertEquals(Buffer.buffer("8080"), get(ServiceAddress.create("svc")));
    ctx.assertEquals(Buffer.buffer("8081"), get(ServiceAddress.create("svc")));
    ctx.assertEquals(Buffer.buffer("8082"), get(ServiceAddress.create("svc")));
    ctx.assertEquals(Buffer.buffer("8080"), get(ServiceAddress.create("svc")));
    ctx.assertEquals(Buffer.buffer("8081"), get(ServiceAddress.create("svc")));
    ctx.assertEquals(Buffer.buffer("8082"), get(ServiceAddress.create("svc")));
  }

  @Test
  public void testLeastRequests(TestContext ctx) throws Exception {
    List<HttpServerRequest> requests = Collections.synchronizedList(new ArrayList<>());
    List<SocketAddress> pods = startPods(3, requests::add);
    MockController controller = new MockController();
    ServiceResolver resolver = MockResolver.create(controller).withLoadBalancer(LoadBalancer.LEAST_REQUESTS);
    client = vertx
      .httpClientBuilder()
      .withAddressResolver(resolver)
      .build();
    controller.register("svc", pods);
    AtomicInteger count = new AtomicInteger();
    for (int i = 0;i < 6;i++) {
      Future<Buffer> fut = client.request(new RequestOptions().setServer(ServiceAddress.create("svc"))).compose(req -> {
        count.incrementAndGet();
        return req.send().compose(HttpClientResponse::body);
      });
      fut.andThen(ar -> count.decrementAndGet());
    }
    assertWaitUntil(() -> requests.size() == 6);
    for (int i = 0;i < requests.size();i++) {
      HttpServerRequest request = requests.get(i);
      if (request.localAddress().port() == 8080) {
        request.response().end();
      }
    }
    assertWaitUntil(() -> count.get() == 4);
    for (int i = 0;i < 2;i++) {
      Future<Integer> fut = client.request(new RequestOptions().setServer(ServiceAddress.create("svc"))).map(req -> req.connection().remoteAddress().port());
      ctx.assertEquals(8080, fut.toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS));
    }
  }
}
