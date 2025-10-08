package io.vertx.tests.kube;

import com.dajudge.kindcontainer.KubernetesContainer;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.PodResource;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import io.vertx.core.net.SocketAddress;
import io.vertx.serviceresolver.ServiceAddress;
import junit.framework.AssertionFailedError;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

import static io.fabric8.kubernetes.client.Config.fromKubeconfig;

public class KubernetesMocking {

  private final KubernetesClient client;
  private final int port;
  private final Config config;

  public KubernetesMocking(KubernetesServer server) {

    NamespacedKubernetesClient client = server.getClient();
    int port;
    try {
      port = new URL(client.getConfiguration().getMasterUrl()).getPort();
    } catch (MalformedURLException e) {
      AssertionFailedError afe = new AssertionFailedError();
      afe.initCause(e);
      throw afe;
    }

    this.client = client;
    this.port = port;
    this.config = client.getConfiguration();
  }

  public KubernetesMocking(KubernetesContainer<?> k8s) throws Exception {
    Config cfg = fromKubeconfig(k8s.getKubeconfig());

    KubernetesClient client2 = new KubernetesClientBuilder()
      .withConfig(cfg).build();

    client2.serviceAccounts()
      .create(new ServiceAccountBuilder()
        .withNewMetadata()
        .withName("default")
        .withNamespace("default")
        .endMetadata()
        .build());

    List<String> namespaces = client2
      .namespaces()
      .list()
      .getItems()
      .stream()
      .map(sa -> sa.getMetadata().getName())
      .collect(Collectors.toList());

    List<String> serviceAccounts = client2
      .serviceAccounts()
      .list()
      .getItems()
      .stream()
      .map(sa -> sa.getMetadata().getName() + "/" + sa.getMetadata().getNamespace())
      .collect(Collectors.toList());

    URL url = new URL(cfg.getMasterUrl());

    this.client = ((NamespacedKubernetesClient)client2).inNamespace("default");
    this.port = url.getPort();
    this.config = fromKubeconfig(k8s.getKubeconfig());
  }

  public Config config() {
    return config;
  }

  public int port() {
    return port;
  }

  public String defaultNamespace() {
    return client.getNamespace();
  }

  public KubernetesClient client() {
    return client;
  }

  //  void registerKubernetesResources(String serviceName, String namespace, List<SocketAddress> ips) {
//    buildAndRegisterKubernetesService(serviceName, namespace, true, ips);
//    ips.forEach(ip -> buildAndRegisterBackendPod(serviceName, namespace, true, ip));
//  }

  Endpoints buildAndRegisterKubernetesService(ServiceAddress service, String namespace, KubeOp op, List<SocketAddress> ipAddresses) {
    return buildAndRegisterKubernetesService(service.name(), namespace, op, ipAddresses);
  }

  Endpoints buildAndRegisterKubernetesService(String applicationName,
                                              String namespace,
                                              KubeOp op,
                                              List<SocketAddress> ipAddresses) {
    return buildAndRegisterKubernetesService(
      applicationName,
      namespace,
      op,
      ipAddresses
        .stream()
        .map(ipAddress -> new KubeEndpoint(ipAddress.host(), ipAddress.port()))
        .collect(Collectors.toList())
        .toArray(KubeEndpoint[]::new)
    );
  }

  Endpoints buildAndRegisterKubernetesService(String applicationName,
                                              String namespace,
                                              KubeOp op,
                                              KubeEndpoint... endpoints_) {
    Map<String, String> serviceLabels = new HashMap<>();
    serviceLabels.put("app.kubernetes.io/name", applicationName);
    serviceLabels.put("app.kubernetes.io/version", "1.0");

    EndpointsBuilder endpointsBuilder = new EndpointsBuilder()
      .withNewMetadata().withName(applicationName).withLabels(serviceLabels).endMetadata();

    for (KubeEndpoint endpoint : endpoints_) {
      EndpointSubsetBuilder builder = new EndpointSubsetBuilder();
      StringBuilder sb = new StringBuilder(applicationName)
        .append('-')
        .append(endpoint.ip.replace(".", ""));
      endpoint.ports.forEach((port, name) -> {
        sb.append('-').append(port);
        EndpointPortBuilder portBuilder = new EndpointPortBuilder().withPort(port).withProtocol("TCP");
        if (name != null && !name.isEmpty()) {
          portBuilder.withName(name);
        }
        builder.addToPorts(portBuilder.build());
      });
      ObjectReference targetRef = new ObjectReference(null, null, "Pod",
        sb.toString(), namespace, null, UUID.randomUUID().toString());
      builder.withAddresses(new EndpointAddressBuilder().withIp(endpoint.ip).withTargetRef(targetRef).build());
      endpointsBuilder.addToSubsets(builder.build());
    }

    NonNamespaceOperation<Endpoints, EndpointsList, Resource<Endpoints>> endpoints;
    if (namespace != null) {
      endpoints = client.endpoints().inNamespace(namespace);
    } else {
      endpoints = client.endpoints();
    }
    Resource<Endpoints> resource = endpoints.resource(endpointsBuilder.build());
    switch (op) {
      case CREATE:
        resource.create();
        break;
      case UPDATE:
        resource.update();
        break;
      case DELETE:
        resource.delete();
        break;
    }
    return endpointsBuilder.build();
  }

  List<Pod> buildAndRegisterBackendPod(ServiceAddress svc, String namespace, KubeOp op, List<SocketAddress> ips) {
    return buildAndRegisterBackendPod(svc.name(), namespace, op, ips);
  }

  List<Pod> buildAndRegisterBackendPod(String name, String namespace, KubeOp op, List<SocketAddress> ips) {

    Map<String, String> serviceLabels = new HashMap<>();
    serviceLabels.put("app.kubernetes.io/name", name);
    serviceLabels.put("app.kubernetes.io/version", "1.0");

    List<Pod> ret = new ArrayList<>();

    for (SocketAddress ip : ips) {
      Map<String, String> podLabels = new HashMap<>(serviceLabels);
      podLabels.put("ui", "ui-" + ipAsSuffix(ip));
      Pod backendPod = new PodBuilder()
        .withNewMetadata().withName(name + "-" + ipAsSuffix(ip))
        .withLabels(podLabels)
        .withNamespace(namespace)
        .endMetadata()
        .withNewSpec()
        .withContainers(new ContainerBuilder()
          .withName("frontend")
          .withImage("clustering-kubernetes/frontend:latest")
          .withPorts(new ContainerPortBuilder().withContainerPort(ip.port()).build())
          .build())
        .endSpec()
        .build();
      NonNamespaceOperation<Pod, PodList, PodResource> existingPods;
      if (namespace != null) {
        existingPods = client.pods().inNamespace(namespace);
      } else {
        existingPods = client.pods();
      }
      PodResource resource = existingPods.resource(backendPod);
      switch (op) {
        case CREATE:
          resource.create();
          break;
        case UPDATE:
          resource.update();
          break;
        case DELETE:
          resource.delete();
          break;
      }
      ret.add(backendPod);
    }

    return ret;
  }

  String ipAsSuffix(SocketAddress ipAddress) {
    return ipAddress.host().replace(".", "") + "-" + ipAddress.port();
  }
}
