package io.vertx.serviceresolver.kube;

import io.vertx.serviceresolver.ServiceAddress;
import io.vertx.serviceresolver.kube.impl.KubernetesServiceAddress;

import java.util.Objects;

/**
 * <p>Build a {@link ServiceAddress} for Kubernetes capable of distinguish a service endpoint
 * by their <a href="https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.26/#endpointport-v1-core">port</a>.</p>
 *
 * <p>This is useful when dealing with pods exposing multiple ports.</p>
 */
public class KubernetesServiceAddressBuilder {

  public static KubernetesServiceAddressBuilder of(String name) {
    return new KubernetesServiceAddressBuilder(name);
  }

  private final String name;
  private int portNumber = 0;
  private String portName = null;

  public KubernetesServiceAddressBuilder(String name) {
    this.name = Objects.requireNonNull(name);
  }

  /**
   * @return the fully build service address
   */
  public ServiceAddress build() {
    return new KubernetesServiceAddress(name, portNumber, portName);
  }

  /**
   * Specify a port {@code number}, otherwise any port will be matched.
   *
   * @param number the port number, must be positive
   * @return this builder
   */
  public KubernetesServiceAddressBuilder withPortNumber(int number) {
    if (number < 0) {
      throw new IllegalArgumentException("Port number must be a positive integer");
    }
    this.portNumber = number;
    return this;
  }

  /**
   * Specify a TCP port {@code name}, otherwise any port will be matched.
   *
   * @param name the port name
   * @return this builder
   */
  public KubernetesServiceAddressBuilder withPortName(String name) {
    this.portName = name;
    return this;
  }
}
