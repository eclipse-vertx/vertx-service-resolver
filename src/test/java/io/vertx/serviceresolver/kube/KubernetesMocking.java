package io.vertx.serviceresolver.kube;

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
import junit.framework.AssertionFailedError;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

import static io.fabric8.kubernetes.client.Config.fromKubeconfig;

public class KubernetesMocking {

  private final KubernetesClient client;
  private final int port;

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

    this.client = ((NamespacedKubernetesClient)client2).inNamespace("default");
    this.port = -1;
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

  Endpoints buildAndRegisterKubernetesService(String applicationName, String namespace, KubeOp op, List<SocketAddress> ipAdresses) {

    Map<String, String> serviceLabels = new HashMap<>();
    serviceLabels.put("app.kubernetes.io/name", applicationName);
    serviceLabels.put("app.kubernetes.io/version", "1.0");

    Map<Integer, List<EndpointAddress>> endpointAddressesMap = new LinkedHashMap<>();
    for (SocketAddress sa : ipAdresses) {
      List<EndpointAddress> endpointAddresses = endpointAddressesMap.compute(sa.port(), (integer, addresses) -> {
        if (addresses == null) {
          addresses = new ArrayList<>();
        }
        return addresses;
      });
      ObjectReference targetRef = new ObjectReference(null, null, "Pod",
        applicationName + "-" + ipAsSuffix(sa), namespace, null, UUID.randomUUID().toString());
      EndpointAddress endpointAddress = new EndpointAddressBuilder().withIp(sa.host()).withTargetRef(targetRef)
        .build();
      endpointAddresses.add(endpointAddress);
    }

    EndpointsBuilder endpointsBuilder = new EndpointsBuilder()
      .withNewMetadata().withName(applicationName).withLabels(serviceLabels).endMetadata();

    endpointAddressesMap.forEach((port, addresses) -> {
      endpointsBuilder.addToSubsets(new EndpointSubsetBuilder().withAddresses(addresses)
        .addToPorts(new EndpointPort[] { new EndpointPortBuilder().withPort(port).withProtocol("TCP").build() })
        .build());
    });

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

  Pod buildAndRegisterBackendPod(String name, String namespace, KubeOp op, SocketAddress ip) {

    Map<String, String> serviceLabels = new HashMap<>();
    serviceLabels.put("app.kubernetes.io/name", name);
    serviceLabels.put("app.kubernetes.io/version", "1.0");

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
    NonNamespaceOperation<Pod, PodList, PodResource> pods;
    if (namespace != null) {
      pods = client.pods().inNamespace(namespace);
    } else {
      pods = client.pods();
    }
    PodResource resource = pods.resource(backendPod);
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
    return backendPod;
  }

  String ipAsSuffix(SocketAddress ipAddress) {
    return ipAddress.host().replace(".", "") + "-" + ipAddress.port();
  }
}
