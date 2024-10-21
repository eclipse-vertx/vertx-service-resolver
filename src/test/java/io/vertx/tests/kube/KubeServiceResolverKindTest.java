package io.vertx.tests.kube;

import com.dajudge.kindcontainer.ApiServerContainer;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.http.TlsVersion;
import io.netty.util.NetUtil;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.http.WebSocketClientOptions;
import io.vertx.core.net.*;
import io.vertx.serviceresolver.kube.KubeResolver;
import io.vertx.serviceresolver.kube.KubeResolverOptions;
import org.junit.Rule;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.fabric8.kubernetes.client.Config.fromKubeconfig;

public class KubeServiceResolverKindTest extends KubeServiceResolverTestBase {

  @Rule
//  public static K3sContainer<?> K8S = new K3sContainer<>();
  public ApiServerContainer<?> K8S = new ApiServerContainer<>();

  public void setUp() throws Exception {
    super.setUp();

    kubernetesMocking = new KubernetesMocking(K8S);

    Config cfg = fromKubeconfig(K8S.getKubeconfig());
    URL url = new URL(cfg.getMasterUrl());
    HttpClientOptions httpClientOptions = new HttpClientOptions();
    WebSocketClientOptions wsClientOptions = new WebSocketClientOptions();
    if (cfg.getTlsVersions() != null && cfg.getTlsVersions().length > 0) {
      Stream.of(cfg.getTlsVersions()).map(TlsVersion::javaName).forEach(httpClientOptions::addEnabledSecureTransportProtocol);
      Stream.of(cfg.getTlsVersions()).map(TlsVersion::javaName).forEach(wsClientOptions::addEnabledSecureTransportProtocol);
    }
    if (cfg.isHttp2Disable()) {
      httpClientOptions.setProtocolVersion(HttpVersion.HTTP_1_1);
    }
    Buffer caCert = Buffer.buffer(Base64.getDecoder().decode(cfg.getCaCertData()));
    Buffer clientKey = Buffer.buffer(Base64.getDecoder().decode(cfg.getClientKeyData()));
    Buffer clientCert = Buffer.buffer(Base64.getDecoder().decode(cfg.getClientCertData()));
    KeyCertOptions keyCerts = new PemKeyCertOptions().addKeyValue(clientKey).addCertValue(clientCert);
    TrustOptions trust = new PemTrustOptions().addCertValue(caCert);
    httpClientOptions
      .setSsl(true)
      .setKeyCertOptions(keyCerts)
      .setTrustOptions(trust);
    wsClientOptions
      .setSsl(true)
      .setKeyCertOptions(keyCerts)
      .setTrustOptions(trust);
    KubeResolverOptions options = new KubeResolverOptions()
      .setNamespace(kubernetesMocking.defaultNamespace())
      .setServer(SocketAddress.inetSocketAddress(url.getPort(), url.getHost()))
      .setHttpClientOptions(httpClientOptions)
      .setWebSocketClientOptions(wsClientOptions);
    client = vertx.httpClientBuilder().withAddressResolver(KubeResolver.create(options)).build();
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
