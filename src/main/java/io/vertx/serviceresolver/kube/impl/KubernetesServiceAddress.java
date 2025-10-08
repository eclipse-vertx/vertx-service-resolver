package io.vertx.serviceresolver.kube.impl;

import io.vertx.serviceresolver.ServiceAddress;

public class KubernetesServiceAddress implements ServiceAddress {

  final String name;
  final int portNumber;
  final String portName;

  public KubernetesServiceAddress(String name, int portNumber, String portName) {
    this.name = name;
    this.portNumber = portNumber;
    this.portName = portName;
  }

  @Override
  public String name() {
    return name;
  }
}
