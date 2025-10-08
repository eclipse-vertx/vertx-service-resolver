package io.vertx.tests.kube;

import java.util.Collections;
import java.util.Map;

public class KubeEndpoint {

  final String ip;
  final Map<Integer, String> ports;

  public KubeEndpoint(String ip, int port) {
    this.ip = ip;
    this.ports = Collections.singletonMap(port, null);
  }

  public KubeEndpoint(String ip, Map<Integer, String> ports) {
    this.ip = ip;
    this.ports = ports;
  }
}
