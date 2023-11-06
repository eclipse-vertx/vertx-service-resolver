package io.vertx.serviceresolver;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.*;
import io.vertx.core.net.SocketAddress;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import junit.framework.AssertionFailedError;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@RunWith(VertxUnitRunner.class)
public abstract class ServiceResolverTestBase {

  protected Vertx vertx;
  protected HttpClient client;
  protected List<HttpServer> pods;

  @Before
  public void setUp() throws Exception {
    vertx = Vertx.vertx();
    pods = new ArrayList<>();
  }

  @After
  public void tearDown() throws Exception {
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

    Future<Buffer> fut = client
      .request(new RequestOptions()
        .setServer(addr))
      .compose(req -> req.send()
        .compose(HttpClientResponse::body));
    try {
      return fut.toCompletionStage().toCompletableFuture().get(20, TimeUnit.SECONDS);
    } catch (ExecutionException e) {
      Throwable cause = e.getCause();
      if (cause instanceof RuntimeException) {
        throw ((RuntimeException) cause);
      } else {
        throw new UndeclaredThrowableException(cause, cause.getMessage());
      }
    } catch (TimeoutException | InterruptedException e) {
      throw e;
    }
  }
}
