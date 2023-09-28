package io.vertx.serviceresolver.kube;

import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import io.vertx.core.Handler;
import io.vertx.core.http.*;
import io.vertx.core.net.SocketAddress;
import io.vertx.ext.unit.TestContext;
import io.vertx.serviceresolver.HttpProxy;
import io.vertx.serviceresolver.ServiceAddress;
import io.vertx.serviceresolver.kube.impl.KubeResolverImpl;
import org.junit.Rule;
import org.junit.Test;

import java.net.InetAddress;
import java.util.*;

public class KubeServiceResolverMockTest extends KubeServiceResolverTestBase {

  @Rule
  public KubernetesServer server = new KubernetesServer(false, true, InetAddress.getLoopbackAddress(), 8443, Collections.emptyList());

  private HttpProxy proxy;

  public void setUp() throws Exception {
    super.setUp();
    kubernetesMocking = new KubernetesMocking(server);

    proxy = new HttpProxy(vertx);
    proxy.origin(SocketAddress.inetSocketAddress(kubernetesMocking.port(), "localhost"));
    proxy.port(1234);
    proxy.start();

    KubeResolverOptions options = new KubeResolverOptions()
      .setNamespace(kubernetesMocking.defaultNamespace())
      .setHost("localhost")
      .setPort(1234)
      .setHttpClientOptions(new HttpClientOptions().setSsl(true).setTrustAll(true))
      .setWebSocketClientOptions(new WebSocketClientOptions().setSsl(true).setTrustAll(true));

    KubeResolver resolver = KubeResolver.create(vertx, options);
    client = vertx.httpClientBuilder()
      .withAddressResolver(resolver)
      .build();
  }

  @Test
  public void testDispose(TestContext should) throws Exception {
    Handler<HttpServerRequest> server = req -> {
      req.response().end("" + req.localAddress().port());
    };
    List<SocketAddress> pods = startPods(1, server);
    String serviceName = "svc";
    kubernetesMocking.buildAndRegisterBackendPod(serviceName, kubernetesMocking.defaultNamespace(), KubeOp.CREATE, pods.get(0));
    kubernetesMocking.buildAndRegisterKubernetesService(serviceName, kubernetesMocking.defaultNamespace(), KubeOp.CREATE, pods);
    should.assertEquals("8080", get(ServiceAddress.create("svc")).toString());
    assertWaitUntil(() -> proxy.webSockets().size() == 1);
    stopPods(pod -> true);
    assertWaitUntil(() -> proxy.webSockets().size() == 0);
  }

  @Test
  public void testReconnectWebSocket(TestContext should) throws Exception {
    Handler<HttpServerRequest> server = req -> {
      req.response().end("" + req.localAddress().port());
    };
    List<SocketAddress> pods = startPods(2, server);
    String serviceName = "svc";
    kubernetesMocking.buildAndRegisterBackendPod(serviceName, kubernetesMocking.defaultNamespace(), KubeOp.CREATE, pods.get(0));
    kubernetesMocking.buildAndRegisterKubernetesService(serviceName, kubernetesMocking.defaultNamespace(), KubeOp.CREATE, pods.subList(0, 1));
    should.assertEquals("8080", get(ServiceAddress.create("svc")).toString());
    assertWaitUntil(() -> proxy.webSockets().size() == 1);
    WebSocketBase ws = proxy.webSockets().iterator().next();
    ws.close();
    assertWaitUntil(() -> proxy.webSockets().size() == 1 && !proxy.webSockets().contains(ws));
    kubernetesMocking.buildAndRegisterBackendPod(serviceName, kubernetesMocking.defaultNamespace(), KubeOp.CREATE, pods.get(1));
    kubernetesMocking.buildAndRegisterKubernetesService(serviceName, kubernetesMocking.defaultNamespace(), KubeOp.UPDATE, pods);
    assertWaitUntil(() -> get(ServiceAddress.create("svc")).toString().equals("8081"));
  }
}
