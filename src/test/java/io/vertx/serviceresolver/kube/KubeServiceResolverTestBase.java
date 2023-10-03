package io.vertx.serviceresolver.kube;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.net.SocketAddress;
import io.vertx.ext.unit.TestContext;
import io.vertx.serviceresolver.ServiceAddress;
import io.vertx.serviceresolver.ServiceResolverTestBase;
import org.junit.Test;

import java.util.List;

public abstract class KubeServiceResolverTestBase extends ServiceResolverTestBase {

  protected KubernetesMocking kubernetesMocking;

  @Test
  public void testSimple(TestContext should) throws Exception {
    List<SocketAddress> pods = startPods(3, req -> {
      req.response().end("" + req.localAddress().port());
    });
    String serviceName = "svc";
    kubernetesMocking.buildAndRegisterBackendPod(serviceName, kubernetesMocking.defaultNamespace(), KubeOp.CREATE, pods.get(0));
    kubernetesMocking.buildAndRegisterBackendPod(serviceName, kubernetesMocking.defaultNamespace(), KubeOp.CREATE, pods.get(1));
    kubernetesMocking.buildAndRegisterBackendPod(serviceName, kubernetesMocking.defaultNamespace(), KubeOp.CREATE, pods.get(2));
    kubernetesMocking.buildAndRegisterKubernetesService(serviceName, kubernetesMocking.defaultNamespace(), KubeOp.CREATE, pods);
    should.assertEquals("8080", get(ServiceAddress.create("svc")).toString());
  }

  @Test
  public void testNoPods(TestContext should) throws Exception {
    try {
      get(ServiceAddress.create("svc"));
    } catch (Exception e) {
      should.assertEquals("No addresses available for svc", e.getMessage());
    }
  }

  @Test
  public void testSelect(TestContext should) throws Exception {
    List<SocketAddress> pods = startPods(3, req -> {
      req.response().end("" + req.localAddress().port());
    });
    String serviceName1 = "svc";
    String serviceName2 = "svc2";
    String serviceName3 = "svc3";
    kubernetesMocking.buildAndRegisterBackendPod(serviceName1, kubernetesMocking.defaultNamespace(), KubeOp.CREATE, pods.get(0));
    kubernetesMocking.buildAndRegisterBackendPod(serviceName2, kubernetesMocking.defaultNamespace(), KubeOp.CREATE, pods.get(1));
    kubernetesMocking.buildAndRegisterKubernetesService(serviceName1, kubernetesMocking.defaultNamespace(), KubeOp.CREATE, pods.subList(0, 1));
    kubernetesMocking.buildAndRegisterKubernetesService(serviceName2, kubernetesMocking.defaultNamespace(), KubeOp.CREATE, pods.subList(1, 2));
    should.assertEquals("8080", get(ServiceAddress.create("svc")).toString());
    should.assertEquals("8080", get(ServiceAddress.create("svc")).toString());
    Thread.sleep(500); // Pause for some time to allow WebSocket to not concurrently run with updates
    kubernetesMocking.buildAndRegisterBackendPod(serviceName3, kubernetesMocking.defaultNamespace(), KubeOp.CREATE, pods.get(2));
    kubernetesMocking.buildAndRegisterKubernetesService(serviceName3, kubernetesMocking.defaultNamespace(), KubeOp.CREATE, pods.subList(2, 3));
    Thread.sleep(500); // Pause for some time to allow WebSocket to get changes
    should.assertEquals("8080", get(ServiceAddress.create("svc")).toString());
    should.assertEquals("8080", get(ServiceAddress.create("svc")).toString());
    should.assertEquals("8080", get(ServiceAddress.create("svc")).toString());
  }

  @Test
  public void testUpdate(TestContext should) throws Exception {
    Handler<HttpServerRequest> server = req -> {
      req.response().end("" + req.localAddress().port());
    };
    List<SocketAddress> pods = startPods(2, server);
    String serviceName = "svc";
    kubernetesMocking.buildAndRegisterBackendPod(serviceName, kubernetesMocking.defaultNamespace(), KubeOp.CREATE, pods.get(0));
    kubernetesMocking.buildAndRegisterKubernetesService(serviceName, kubernetesMocking.defaultNamespace(), KubeOp.CREATE, pods.subList(0, 1));
    should.assertEquals("8080", get(ServiceAddress.create("svc")).toString());
    kubernetesMocking.buildAndRegisterBackendPod(serviceName, kubernetesMocking.defaultNamespace(), KubeOp.CREATE, pods.get(1));
    kubernetesMocking.buildAndRegisterKubernetesService(serviceName, kubernetesMocking.defaultNamespace(), KubeOp.UPDATE, pods);
    assertWaitUntil(() -> get(ServiceAddress.create("svc")).toString().equals("8081"));
  }

  @Test
  public void testDeletePod(TestContext should) throws Exception {
    Handler<HttpServerRequest> server = req -> {
      req.response().end("" + req.localAddress().port());
    };
    List<SocketAddress> pods = startPods(2, server);
    String serviceName = "svc";
    kubernetesMocking.buildAndRegisterBackendPod(serviceName, kubernetesMocking.defaultNamespace(), KubeOp.CREATE, pods.get(0));
    kubernetesMocking.buildAndRegisterBackendPod(serviceName, kubernetesMocking.defaultNamespace(), KubeOp.CREATE, pods.get(1));
    kubernetesMocking.buildAndRegisterKubernetesService(serviceName, kubernetesMocking.defaultNamespace(), KubeOp.CREATE, pods);
    should.assertEquals("8080", get(ServiceAddress.create("svc")).toString());
    should.assertEquals("8081", get(ServiceAddress.create("svc")).toString());
    kubernetesMocking.buildAndRegisterBackendPod(serviceName, kubernetesMocking.defaultNamespace(), KubeOp.DELETE, pods.get(1));
    kubernetesMocking.buildAndRegisterKubernetesService(serviceName, kubernetesMocking.defaultNamespace(), KubeOp.UPDATE, pods.subList(0, 1));
    long now = System.currentTimeMillis();
    again:
    while (true) {
      should.assertTrue(System.currentTimeMillis() - now < 20_000);
      for (int i = 0;i < 3;i++) {
        if (!"8080".equals(get(ServiceAddress.create("svc")).toString())) {
          Thread.sleep(10);
          continue again;
        }
      }
      break;
    }
  }

/*
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
*/

/*
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
*/
}
