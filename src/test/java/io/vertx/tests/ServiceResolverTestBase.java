package io.vertx.tests;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.*;
import io.vertx.core.net.AddressResolver;
import io.vertx.core.net.SocketAddress;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.serviceresolver.ServiceAddress;
import junit.framework.AssertionFailedError;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.junit.Assert.fail;

@RunWith(VertxUnitRunner.class)
public abstract class ServiceResolverTestBase {

  protected Vertx vertx;
  protected List<HttpServer> pods;
  protected Client client;

  @Before
  public void setUp() throws Exception {
    vertx = Vertx.vertx();
    pods = new ArrayList<>();
  }

  @After
  public void tearDown() throws Exception {
    if (client != null) {
      client.close();
    }
    vertx.close()
      .toCompletionStage()
      .toCompletableFuture()
      .get(20, TimeUnit.SECONDS);
  }

  protected void stopPods(Predicate<HttpServer> pred) throws Exception {
    Set<HttpServer> stopped = new HashSet<>();
    for (HttpServer pod : pods) {
      if (pred.test(pod)) {
        stopped.add(pod);
        pod.close().toCompletionStage().toCompletableFuture().get(20, TimeUnit.SECONDS);
      }
    }
    pods.removeAll(stopped);
  }

  protected List<SocketAddress> startPods(int numPods, Handler<HttpServerRequest> service) throws Exception {
    return startPods(numPods, "0.0.0.0", service);
  }

  protected List<SocketAddress> startPods(int numPods, String bindAddress, Handler<HttpServerRequest> service) throws Exception {
    int basePort = pods.isEmpty() ? 8080 : pods.get(pods.size() - 1).actualPort() + 1;
    List<HttpServer> started = new ArrayList<>();
    for (int i = 0;i < numPods;i++) {
      HttpServer pod = vertx
        .createHttpServer()
        .requestHandler(service);
      started.add(pod);
      pod.listen(basePort + i, bindAddress)
        .toCompletionStage()
        .toCompletableFuture()
        .get(20, TimeUnit.SECONDS);
    }
    pods.addAll(started);
    return started
      .stream()
      .map(s -> SocketAddress.inetSocketAddress(s.actualPort(), bindAddress))
      .collect(Collectors.toList());
  }

  protected void assertWaitUntil(Callable<Boolean> cond) throws Exception {
    long now = System.currentTimeMillis();
    while (!cond.call()) {
      if (System.currentTimeMillis() - now > 20_000) {
        throw new AssertionFailedError();
      }
    }
  }

  protected Buffer get(ServiceAddress addr) throws Exception {
    Client c;
    synchronized (this) {
      c = client;
      if (c == null) {
        c = client();
        this.client = c;
      }
    }
    return c.get(addr);
  }

  protected Client client() {
    return client(resolver());
  }

  protected Client client(AddressResolver<?> resolver) {
    return new Client(resolver);
  }

  protected abstract AddressResolver<?> resolver();

  protected class Client {

    protected HttpClient client;

    public Client(AddressResolver<?> resolver) {
      this.client = vertx.httpClientBuilder()
        .withAddressResolver(resolver)
        .build();
    }

    public Buffer get(ServiceAddress addr) throws Exception {
      Future<Buffer> fut = client
        .request(new RequestOptions()
          .setServer(addr))
        .compose(req -> req.send()
          .compose(HttpClientResponse::body));
      return fut.await(20, TimeUnit.SECONDS);
    }

    public void close() {
      client.close().await();
    }
  }

  protected void checkEndpoints(ServiceAddress svc, String... values) {
    Set<String> expected = new HashSet<>(Arrays.asList(values));
    int retries = 5;
    for (int r = 0;r < retries;r++) {
      Set<String> found = new HashSet<>();
      for (int i = 0;i < values.length * 10;i++) {
        try {
          found.add(get(svc).toString());
        } catch (Exception ignore) {
        }
      }
      if (found.equals(expected)) {
        return;
      }
      try {
        Thread.sleep(10);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
    fail();
  }
}
