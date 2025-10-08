package io.vertx.tests.kube;

import io.fabric8.kubernetes.client.Config;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.*;
import io.vertx.core.net.*;
import io.vertx.ext.unit.TestContext;
import io.vertx.serviceresolver.ServiceAddress;
import io.vertx.serviceresolver.kube.KubeResolver;
import io.vertx.serviceresolver.kube.KubeResolverOptions;
import io.vertx.tests.HttpProxy;
import io.vertx.tests.ServiceResolverTestBase;
import org.junit.Ignore;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public abstract class KubeServiceResolverTestBase extends ServiceResolverTestBase {

  protected KubernetesMocking kubernetesMocking;
  protected HttpProxy proxy;
  protected KubeResolverOptions options;

  @Override
  public void setUp() throws Exception {
    super.setUp();

    Config cfg = kubernetesMocking.config();

    proxy = new HttpProxy(vertx);
    proxy.origin(SocketAddress.inetSocketAddress(kubernetesMocking.port(), "localhost"));
    proxy.port(1234);

    String caCertData = cfg.getCaCertData();
    String clientKeyData = cfg.getClientKeyData();
    String clientCertData = cfg.getClientCertData();
    if (caCertData != null && clientKeyData != null && clientCertData != null) {
      Buffer caCert = Buffer.buffer(Base64.getDecoder().decode(caCertData));
      Buffer clientKey = Buffer.buffer(Base64.getDecoder().decode(clientKeyData));
      Buffer clientCert = Buffer.buffer(Base64.getDecoder().decode(clientCertData));
      KeyCertOptions keyCerts = new PemKeyCertOptions().addKeyValue(clientKey).addCertValue(clientCert);
      TrustOptions trust = new PemTrustOptions().addCertValue(caCert);
      proxy.keyCertOptions(keyCerts);
      proxy.trustOptions(trust);
      proxy.ssl(true);
    }

    if (cfg.isHttp2Disable()) {
      proxy.protocol(HttpVersion.HTTP_1_1);
    }

    proxy.start();

    options = new KubeResolverOptions()
      .setNamespace(kubernetesMocking.defaultNamespace())
      .setServer(SocketAddress.inetSocketAddress(1234, "localhost"))
      .setHttpClientOptions(new HttpClientOptions().setSsl(false))
      .setWebSocketClientOptions(new WebSocketClientOptions().setSsl(false));
  }

  @Override
  public void tearDown() throws Exception {
    proxy.close();
    super.tearDown();
  }

  @Override
  protected AddressResolver<?> resolver() {
    return KubeResolver.create(options);
  }

  @Test
  public void testSimple(TestContext should) throws Exception {
    List<SocketAddress> pods = startPods(3, req -> {
      req.response().end("" + req.localAddress().port());
    });
    ServiceAddress service = ServiceAddress.of("svc");
    kubernetesMocking.buildAndRegisterBackendPod(service, kubernetesMocking.defaultNamespace(), KubeOp.CREATE, pods);
    kubernetesMocking.buildAndRegisterKubernetesService(service, kubernetesMocking.defaultNamespace(), KubeOp.CREATE, pods);
    should.assertEquals("8080", get(ServiceAddress.of("svc")).toString());
  }

  @Ignore
  @Test
  public void testNoPods(TestContext should) throws Exception {
    try {
      get(ServiceAddress.of("svc"));
    } catch (Exception e) {
      should.assertEquals("No addresses available for svc", e.getMessage());
    }
  }

  @Test
  public void testUpdate() throws Exception {
    Handler<HttpServerRequest> server = req -> {
      req.response().end("" + req.localAddress().port());
    };
    ServiceAddress service = ServiceAddress.of("svc");
    int numPods = 10;
    List<SocketAddress> pods = startPods(numPods, server);
    kubernetesMocking.buildAndRegisterBackendPod(service, kubernetesMocking.defaultNamespace(), KubeOp.CREATE, pods);
    kubernetesMocking.buildAndRegisterKubernetesService(service, kubernetesMocking.defaultNamespace(), KubeOp.CREATE, Collections.emptyList());
    int iterations = 100;
    for (int k = 0;k < iterations;k++) {
      for (int pod = 0;pod < numPods;pod++) {
        kubernetesMocking.buildAndRegisterKubernetesService(service, kubernetesMocking.defaultNamespace(), KubeOp.UPDATE, pods.subList(0, pod));
        List<String> expected = pods.subList(0, pod).stream().map(address -> "" + address.port()).collect(Collectors.toList());
        checkEndpoints(service, expected.toArray(new String[0]));
      }
    }
  }

  @Test
  public void testDeletePod() throws Exception {
    Handler<HttpServerRequest> server = req -> {
      req.response().end("" + req.localAddress().port());
    };
    ServiceAddress svc = ServiceAddress.of("svc");
    List<SocketAddress> pods = startPods(2, server);
    kubernetesMocking.buildAndRegisterBackendPod(svc, kubernetesMocking.defaultNamespace(), KubeOp.CREATE, pods);
    kubernetesMocking.buildAndRegisterKubernetesService(svc, kubernetesMocking.defaultNamespace(), KubeOp.CREATE, Collections.emptyList());
    checkEndpoints(svc);
    kubernetesMocking.buildAndRegisterKubernetesService(svc, kubernetesMocking.defaultNamespace(), KubeOp.UPDATE, pods);
    checkEndpoints(svc, "8080", "8081");
    kubernetesMocking.buildAndRegisterKubernetesService(svc, kubernetesMocking.defaultNamespace(), KubeOp.UPDATE, pods.subList(0, 1));
    checkEndpoints(svc, "8080");
  }

  @Test
  public void testEmptyPods() {
    ServiceAddress svc = ServiceAddress.of("svc");
    kubernetesMocking.buildAndRegisterKubernetesService(svc, kubernetesMocking.defaultNamespace(), KubeOp.CREATE, Collections.emptyList());
    checkEndpoints(svc);
  }

  @Test
  public void testTokenProvider() throws Exception {
    Handler<HttpServerRequest> server = req -> {
      req.response().end("" + req.localAddress().port());
    };
    List<SocketAddress> pods = startPods(2, server);
    ServiceAddress service1 = ServiceAddress.of("svc1");
    ServiceAddress service2 = ServiceAddress.of("svc2");
    kubernetesMocking.buildAndRegisterBackendPod(service1, kubernetesMocking.defaultNamespace(), KubeOp.CREATE, pods.subList(0, 1));
    kubernetesMocking.buildAndRegisterKubernetesService(service1, kubernetesMocking.defaultNamespace(), KubeOp.CREATE, pods.subList(0, 1));
    kubernetesMocking.buildAndRegisterBackendPod(service2, kubernetesMocking.defaultNamespace(), KubeOp.CREATE, pods.subList(1, 2));
    kubernetesMocking.buildAndRegisterKubernetesService(service2, kubernetesMocking.defaultNamespace(), KubeOp.CREATE, pods.subList(1, 2));
    AtomicInteger count = new AtomicInteger();
    Client client = client(KubeResolver.create(options).tokenProvider(() -> "" + count.getAndIncrement()));
    proxy.requestHandler(request -> {
      String token = request.getHeader(HttpHeaders.AUTHORIZATION);
      if ("Bearer 2".equals(token)) {
        return true;
      } else {
        request
          .response()
          .setStatusCode(401)
          .end();
        return false;
      }
    });
    Buffer res = client.get(ServiceAddress.of("svc1"));
    assertEquals("8080", res.toString());
    assertEquals(3, count.get());

    // Check we have cached the correct token
    res = client.get(ServiceAddress.of("svc2"));
    assertEquals("8081", res.toString());
    assertEquals(3, count.get());
  }

  @Test
  public void testIncorrectToken() throws Exception {
    Handler<HttpServerRequest> server = req -> {
      req.response().end("" + req.localAddress().port());
    };
    List<SocketAddress> pods = startPods(1, server);
    ServiceAddress service1 = ServiceAddress.of("svc1");
    kubernetesMocking.buildAndRegisterBackendPod(service1, kubernetesMocking.defaultNamespace(), KubeOp.CREATE, pods.subList(0, 1));
    kubernetesMocking.buildAndRegisterKubernetesService(service1, kubernetesMocking.defaultNamespace(), KubeOp.CREATE, pods.subList(0, 1));
    AtomicInteger count = new AtomicInteger();
    Client client = client(KubeResolver.create(options).tokenProvider(() -> {
      count.getAndIncrement();
      return "fail";
    }));
    proxy.requestHandler(request -> {
      String token = request.getHeader(HttpHeaders.AUTHORIZATION);
      if ("pass".equals(token)) {
        return true;
      } else {
        request
          .response()
          .setStatusCode(401)
          .end();
        return false;
      }
    });
    try {
      client.get(ServiceAddress.of("svc1"));
      fail();
    } catch (Exception e) {
      assertTrue(e.getMessage().contains("401"));
    }
    assertEquals(2, count.get());
  }

  @Test
  public void testWebSocketClose(TestContext should) throws Exception {
    Handler<HttpServerRequest> server = req -> {
      req.response().end("" + req.localAddress().port());
    };
    List<SocketAddress> pods = startPods(2, server);
    ServiceAddress service = ServiceAddress.of("svc");
    kubernetesMocking.buildAndRegisterBackendPod(service, kubernetesMocking.defaultNamespace(), KubeOp.CREATE, pods);
    kubernetesMocking.buildAndRegisterKubernetesService(service, kubernetesMocking.defaultNamespace(), KubeOp.CREATE, pods.subList(0, 1));
    checkEndpoints(service, "8080");
    assertWaitUntil(() -> proxy.webSockets().size() == 1);
    WebSocketBase ws = proxy.webSockets().iterator().next();
    ws.close();
    assertWaitUntil(() -> !proxy.webSockets().contains(ws));
    kubernetesMocking.buildAndRegisterKubernetesService(service, kubernetesMocking.defaultNamespace(), KubeOp.UPDATE, pods);
    checkEndpoints(service, "8080", "8081");
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
