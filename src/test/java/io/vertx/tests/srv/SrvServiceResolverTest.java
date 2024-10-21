package io.vertx.tests.srv;

import io.vertx.core.http.HttpClient;
import io.vertx.core.net.SocketAddress;
import io.vertx.ext.unit.TestContext;
import io.vertx.serviceresolver.ServiceAddress;
import io.vertx.serviceresolver.ServiceResolverClient;
import io.vertx.serviceresolver.srv.SrvResolver;
import io.vertx.serviceresolver.srv.SrvResolverOptions;
import io.vertx.tests.ServiceResolverTestBase;
import io.vertx.test.fakedns.FakeDNSServer;
import org.apache.directory.server.dns.messages.*;
import org.apache.directory.server.dns.store.DnsAttribute;
import org.junit.Test;
import org.apache.directory.server.dns.store.RecordStore;

import java.util.*;

public class SrvServiceResolverTest extends ServiceResolverTestBase {

  private FakeDNSServer dnsServer;
  private final SrvResolverOptions options = new SrvResolverOptions()
    .setServer(SocketAddress.inetSocketAddress(FakeDNSServer.PORT, FakeDNSServer.IP_ADDRESS));

  public void setUp() throws Exception {
    super.setUp();

    dnsServer = new FakeDNSServer();
    dnsServer.start();

    client = createHttpClient(options);
  }

  private HttpClient createHttpClient(SrvResolverOptions options) {
    return vertx.httpClientBuilder().withAddressResolver(SrvResolver.create(options)).build();
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
    dnsServer.store(new RecordStore() {
      @Override
      public Set<ResourceRecord> getRecords(QuestionRecord questionRecord) {
        Set<ResourceRecord> set = new HashSet<>();
        if ("_http._tcp.example.com".equals(questionRecord.getDomainName())) {
          for (int i = 0;i < 2;i++) {
            FakeDNSServer.Record record = new FakeDNSServer.Record(
              "_http._tcp.example.com",
              RecordType.SRV,
              RecordClass.IN,
              100
            )
              .set(DnsAttribute.SERVICE_PRIORITY, 1)
              .set(DnsAttribute.SERVICE_WEIGHT, 1)
              .set(DnsAttribute.SERVICE_PORT, 8080 + i)
              .set(DnsAttribute.DOMAIN_NAME, "localhost");
            set.add(record);
          }
        }
        return set;
      }
    });
    Set<String> set = new HashSet<>(Arrays.asList("8080", "8081"));
    should.assertTrue(set.remove(get(ServiceAddress.of("_http._tcp.example.com.")).toString()));
    should.assertTrue(set.remove(get(ServiceAddress.of("_http._tcp.example.com.")).toString()));
    should.assertEquals(Collections.emptySet(), set);
  }

  @Test
  public void testExpiration1(TestContext should) throws Exception {
    testExpiration(should, 1);
  }

  @Test
  public void testExpirationMinTTL(TestContext should) throws Exception {
    client.close();
    client = createHttpClient(new SrvResolverOptions(options).setMinTTL(1));
    testExpiration(should, 0);
  }

  private void testExpiration(TestContext should, int ttl) throws Exception {
    startPods(4, req -> {
      req.response().end("" + req.localAddress().port());
    });
    List<Integer> ports = Collections.synchronizedList(new ArrayList<>());
    dnsServer.store(new RecordStore() {
      @Override
      public Set<ResourceRecord> getRecords(QuestionRecord questionRecord) {
        Set<ResourceRecord> set = new HashSet<>();
        if ("_http._tcp.example.com".equals(questionRecord.getDomainName())) {
          for (Integer port : ports) {
            FakeDNSServer.Record record = new FakeDNSServer.Record(
              "_http._tcp.example.com",
              RecordType.SRV,
              RecordClass.IN,
              ttl
            )
              .set(DnsAttribute.SERVICE_PRIORITY, 1)
              .set(DnsAttribute.SERVICE_WEIGHT, 1)
              .set(DnsAttribute.SERVICE_PORT, port)
              .set(DnsAttribute.DOMAIN_NAME, "localhost");
            set.add(record);
          }
        }
        return set;
      }
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
    dnsServer.store(new RecordStore() {
      @Override
      public Set<ResourceRecord> getRecords(QuestionRecord questionRecord) {
        Set<ResourceRecord> set = new HashSet<>();
        if ("_http._tcp.example.com".equals(questionRecord.getDomainName())) {
          for (Integer port : ports) {
            FakeDNSServer.Record record = new FakeDNSServer.Record(
              "_http._tcp.example.com",
              RecordType.SRV,
              RecordClass.IN,
              0
            )
              .set(DnsAttribute.SERVICE_PRIORITY, 1)
              .set(DnsAttribute.SERVICE_WEIGHT, 1)
              .set(DnsAttribute.SERVICE_PORT, port)
              .set(DnsAttribute.DOMAIN_NAME, "localhost");
            set.add(record);
          }
        }
        return set;
      }
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
    dnsServer.store(new RecordStore() {
      @Override
      public Set<ResourceRecord> getRecords(QuestionRecord questionRecord) {
        Set<ResourceRecord> set = new HashSet<>();
        if ("_http._tcp.example.com".equals(questionRecord.getDomainName())) {
          for (int i = 0;i < 2;i++) {
            FakeDNSServer.Record record = new FakeDNSServer.Record(
              "_http._tcp.example.com",
              RecordType.SRV,
              RecordClass.IN,
              100
            )
              .set(DnsAttribute.SERVICE_PRIORITY, 1)
              .set(DnsAttribute.SERVICE_WEIGHT, 1)
              .set(DnsAttribute.SERVICE_PORT, 8080 + i)
              .set(DnsAttribute.DOMAIN_NAME, "localhost");
            set.add(record);
          }
        }
        return set;
      }
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
