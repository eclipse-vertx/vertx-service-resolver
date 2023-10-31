package io.vertx.serviceresolver.dns;

import io.vertx.core.Future;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.spi.net.AddressResolver;
import io.vertx.ext.unit.TestContext;
import io.vertx.serviceresolver.ServiceResolverTestBase;
import io.vertx.serviceresolver.dns.impl.DnsResolverImpl;
import io.vertx.serviceresolver.impl.ResolverImpl;
import io.vertx.serviceresolver.loadbalancing.LoadBalancer;
import io.vertx.test.fakedns.FakeDNSServer;
import org.apache.directory.server.dns.messages.*;
import org.apache.directory.server.dns.store.DnsAttribute;
import org.junit.Test;

import java.util.*;

public class DnsServiceResolverTest extends ServiceResolverTestBase {

  private FakeDNSServer dnsServer;
  private AddressResolver resolver;

  public void setUp() throws Exception {
    super.setUp();

    dnsServer = new FakeDNSServer();
    dnsServer.start();

    dnsServer.store(questionRecord -> {
      Set<ResourceRecord> set = new LinkedHashSet<>();
      if ("example.com".equals(questionRecord.getDomainName())) {
        for (int i = 0;i < 2;i++) {
          String ip = "127.0.0." + (i + 1);
          set.add(new ResourceRecord() {
            @Override
            public String getDomainName() {
              return "example.com";
            }
            @Override
            public RecordType getRecordType() {
              return RecordType.A;
            }
            @Override
            public RecordClass getRecordClass() {
              return RecordClass.IN;
            }
            @Override
            public int getTimeToLive() {
              return 100;
            }
            @Override
            public String get(String id) {
              if (id.equals(DnsAttribute.IP_ADDRESS)) {
                return ip;
              }
              return null;
            }
          });
        }
      }
      return set;
    });

    DnsResolverOptions options = new DnsResolverOptions()
      .setHost(FakeDNSServer.IP_ADDRESS)
      .setPort(FakeDNSServer.PORT);

    resolver = new ResolverImpl<>(vertx, LoadBalancer.ROUND_ROBIN, new DnsResolverImpl(options));
  }

  public void tearDown() throws Exception {
    dnsServer.stop();
    super.tearDown();
  }

  @Test
  public void testResolve(TestContext should) {
    Future fut = resolver.resolve(SocketAddress.inetSocketAddress(8080, "example.com"));
    fut.onComplete(should.asyncAssertSuccess(state -> {
      Object e1 = resolver.pickEndpoint(state);
      Object e2 = resolver.pickEndpoint(state);
      SocketAddress addr1 = resolver.addressOf(e1);
      should.assertEquals("127.0.0.1", addr1.host());
      should.assertEquals(8080, addr1.port());
      SocketAddress addr2 = resolver.addressOf(e2);
      should.assertEquals("127.0.0.2", addr2.host());
      should.assertEquals(8080, addr2.port());
    }));
  }
}
