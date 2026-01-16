package io.vertx.tests.srv;

import io.netty.handler.codec.dns.DnsRecord;
import io.vertx.core.net.AddressResolver;
import io.vertx.core.net.SocketAddress;
import io.vertx.ext.unit.TestContext;
import io.vertx.serviceresolver.ServiceAddress;
import io.vertx.serviceresolver.ServiceResolverClient;
import io.vertx.serviceresolver.srv.SrvResolver;
import io.vertx.serviceresolver.srv.SrvResolverOptions;
import io.vertx.test.fakedns.MockDnsServer;
import io.vertx.tests.ServiceResolverTestBase;
import org.junit.Test;

import java.util.*;

public class SrvServiceResolverTest extends ServiceResolverTestBase {

  private MockDnsServer dnsServer;
  private SrvResolverOptions options = new SrvResolverOptions()
    .setServer(SocketAddress.inetSocketAddress(MockDnsServer.PORT, MockDnsServer.IP_ADDRESS));

  public void setUp() throws Exception {
    super.setUp();

    dnsServer = new MockDnsServer();
    dnsServer.start();
  }

  @Override
  protected AddressResolver<?> resolver() {
    return SrvResolver.create(options);
  }

  public void tearDown() throws Exception {
    dnsServer.stop();
    super.tearDown();
  }

  @Test
  public void testResolve(TestContext should) throws Exception {
    startPods(2, req -> {
      req.response().end("" + req.localAddress().port());
    });
    dnsServer.store(question -> {
      List<DnsRecord> list = new ArrayList<>();
      if ("_http._tcp.example.com.".equals(question.name())) {
        for (int i = 0;i < 2;i++) {
          DnsRecord record = MockDnsServer.srv(
            "_http._tcp.example.com.", 100, 1, 1, 8080 + i, "localhost");
          list.add(record);
        }
      }
      return list;
    });
    checkEndpoints(ServiceAddress.of("_http._tcp.example.com."), "8080", "8081");
  }

  @Test
  public void testExpiration1(TestContext should) throws Exception {
    testExpiration(should, 1);
  }

  @Test
  public void testExpirationMinTTL(TestContext should) throws Exception {
    options = new SrvResolverOptions(options).setMinTTL(1);
    testExpiration(should, 0);
  }

  private void testExpiration(TestContext should, int ttl) throws Exception {
    startPods(4, req -> {
      req.response().end("" + req.localAddress().port());
    });
    List<Integer> ports = Collections.synchronizedList(new ArrayList<>());
    dnsServer.store(question -> {
      List<DnsRecord> list = new ArrayList<>();
      if ("_http._tcp.example.com.".equals(question.name())) {
        for (Integer port : ports) {
          DnsRecord record = MockDnsServer.srv(
            "_http._tcp.example.com.",
            ttl,
            1,
            1,
            port,
            "localhost");
          list.add(record);
        }
      }
      return list;
    });
    ports.add(8080);
    ports.add(8081);
    Set<String> set = new HashSet<>(Arrays.asList("8080", "8081"));
    should.assertTrue(set.remove(get(ServiceAddress.of("_http._tcp.example.com.")).toString()));
    should.assertTrue(set.remove(get(ServiceAddress.of("_http._tcp.example.com.")).toString()));
    should.assertEquals(Collections.emptySet(), set);
    ports.clear();
    ports.add(8082);
    ports.add(8083);
    Thread.sleep(1500);
    set = new HashSet<>(Arrays.asList("8082", "8083"));
    should.assertTrue(set.remove(get(ServiceAddress.of("_http._tcp.example.com.")).toString()));
    should.assertTrue(set.remove(get(ServiceAddress.of("_http._tcp.example.com.")).toString()));
    should.assertEquals(Collections.emptySet(), set);
  }

  @Test
  public void testNoCaching(TestContext should) throws Exception {
    startPods(2, req -> {
      req.response().end("" + req.localAddress().port());
    });
    List<Integer> ports = Collections.synchronizedList(new ArrayList<>());
    dnsServer.store(question -> {
      List<DnsRecord> list = new ArrayList<>();
      if ("_http._tcp.example.com.".equals(question.name())) {
        for (Integer port : ports) {
          DnsRecord record = MockDnsServer.srv(
            "_http._tcp.example.com.", 0, 1, 1, port, "localhost");
          list.add(record);
        }
      }
      return list;
    });
    ports.add(8080);
    should.assertEquals("8080", get(ServiceAddress.of("_http._tcp.example.com.")).toString());
    should.assertEquals("8080", get(ServiceAddress.of("_http._tcp.example.com.")).toString());
    ports.clear();
    ports.add(8081);
    should.assertEquals("8081", get(ServiceAddress.of("_http._tcp.example.com.")).toString());
    should.assertEquals("8081", get(ServiceAddress.of("_http._tcp.example.com.")).toString());
  }

  @Test
  public void testServiceResolver(TestContext should) throws Exception {
    dnsServer.store(question -> {
      List<DnsRecord> list = new ArrayList<>();
      if ("_http._tcp.example.com.".equals(question.name())) {
        for (int i = 0;i < 2;i++) {
          DnsRecord record = MockDnsServer.srv(
            "_http._tcp.example.com.", 100, 1, 1, 8080 + i, "localhost");
          list.add(record);
        }
      }
      return list;
    });
    ServiceResolverClient resolver = ServiceResolverClient.create(vertx, options);
    resolver.resolveEndpoint(ServiceAddress.of("_http._tcp.example.com."))
      .onComplete(should.asyncAssertSuccess(res -> {
      should.assertEquals(2, res.servers().size());
      should.assertEquals(8080, res.servers().get(0).address().port());
      should.assertEquals(8081, res.servers().get(1).address().port());
    }));
  }
}
