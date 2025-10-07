package io.vertx.tests.kube;

import com.dajudge.kindcontainer.ApiServerContainer;
import io.netty.util.NetUtil;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.net.*;
import org.junit.Rule;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.*;
import java.util.stream.Collectors;

public class KubeServiceResolverKindTest extends KubeServiceResolverTestBase {

  @Rule
//  public static K3sContainer<?> K8S = new K3sContainer<>();
  public ApiServerContainer<?> K8S = new ApiServerContainer<>();

  public void setUp() throws Exception {
    kubernetesMocking = new KubernetesMocking(K8S);
    super.setUp();
  }

  private String determineHostAddress() {
    for (NetworkInterface ni : NetUtil.NETWORK_INTERFACES) {
      Enumeration<InetAddress> addresses = ni.getInetAddresses();
      while (addresses.hasMoreElements()) {
        InetAddress address = addresses.nextElement();
        if (address instanceof Inet4Address && !address.isLoopbackAddress()) {
          return address.getHostAddress();
        }
      }
    }
    return null;
  }

  @Override
  protected List<SocketAddress> startPods(int numPods, Handler<HttpServerRequest> service) throws Exception {
    String host = determineHostAddress();
    List<SocketAddress> pods = startPods(numPods, host, service);
    return pods.stream().map(sa -> SocketAddress.inetSocketAddress(sa.port(), host)).collect(Collectors.toList());
  }
}
