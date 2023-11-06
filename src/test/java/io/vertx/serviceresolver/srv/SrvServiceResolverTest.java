package io.vertx.serviceresolver.srv;

import io.vertx.core.VertxOptions;
import io.vertx.core.dns.AddressResolverOptions;
import io.vertx.ext.unit.TestContext;
import io.vertx.serviceresolver.ServiceAddress;
import io.vertx.serviceresolver.ServiceResolverTestBase;
import io.vertx.test.fakedns.FakeDNSServer;
import org.apache.directory.server.dns.messages.*;
import org.apache.directory.server.dns.store.DnsAttribute;
import org.junit.Ignore;
import org.junit.Test;
import org.apache.directory.server.dns.store.RecordStore;

import java.util.*;

public class SrvServiceResolverTest extends ServiceResolverTestBase {

  private FakeDNSServer dnsServer;

  public void setUp() throws Exception {
    super.setUp();

    dnsServer = new FakeDNSServer();
    dnsServer.start();

    SrvResolverOptions options = new SrvResolverOptions()
      .setHost(FakeDNSServer.IP_ADDRESS)
      .setPort(FakeDNSServer.PORT);

    client = vertx.httpClientBuilder().withAddressResolver(SrvResolver.create(options)).build();
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
    should.assertTrue(set.remove(get(ServiceAddress.create("_http._tcp.example.com.")).toString()));
    should.assertTrue(set.remove(get(ServiceAddress.create("_http._tcp.example.com.")).toString()));
    should.assertEquals(Collections.emptySet(), set);
  }

  @Test
  public void testExpiration(TestContext should) throws Exception {
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
              1
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
    should.assertTrue(set.remove(get(ServiceAddress.create("_http._tcp.example.com.")).toString()));
    should.assertTrue(set.remove(get(ServiceAddress.create("_http._tcp.example.com.")).toString()));
    should.assertEquals(Collections.emptySet(), set);
    Thread.sleep(1500);
    ports.clear();
    ports.add(8082);
    ports.add(8083);
    set = new HashSet<>(Arrays.asList("8082", "8083"));
    should.assertTrue(set.remove(get(ServiceAddress.create("_http._tcp.example.com.")).toString()));
    should.assertTrue(set.remove(get(ServiceAddress.create("_http._tcp.example.com.")).toString()));
    should.assertEquals(Collections.emptySet(), set);
  }
}
