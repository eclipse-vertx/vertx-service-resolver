package io.vertx.tests.kube;

import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import io.vertx.core.Handler;
import io.vertx.core.http.*;
import io.vertx.core.net.SocketAddress;
import io.vertx.ext.unit.TestContext;
import io.vertx.serviceresolver.ServiceAddress;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import java.net.InetAddress;
import java.util.*;

public class KubeServiceResolverMockTest extends KubeServiceResolverTestBase {

  @Rule
  public KubernetesServer server = new KubernetesServer(false, true, InetAddress.getLoopbackAddress(), 8443, Collections.emptyList());

  public void setUp() throws Exception {
    kubernetesMocking = new KubernetesMocking(server);
    super.setUp();
  }

  @Ignore
  @Test
  public void testDispose(TestContext should) throws Exception {
    Handler<HttpServerRequest> server = req -> {
      req.response().end("" + req.localAddress().port());
    };
    List<SocketAddress> pods = startPods(1, server);
    ServiceAddress service = ServiceAddress.of("svc");
    kubernetesMocking.buildAndRegisterBackendPod(service, kubernetesMocking.defaultNamespace(), KubeOp.CREATE, pods);
    kubernetesMocking.buildAndRegisterKubernetesService(service, kubernetesMocking.defaultNamespace(), KubeOp.CREATE, pods);
    should.assertEquals("8080", get(service).toString());
    assertWaitUntil(() -> proxy.webSockets().size() == 1);
    stopPods(pod -> true);
    assertWaitUntil(() -> proxy.webSockets().size() == 0);
  }
}
