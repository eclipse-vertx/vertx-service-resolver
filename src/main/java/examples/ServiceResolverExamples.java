/*
 * Copyright (c) 2011-2023 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package examples;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.*;
import io.vertx.core.net.AddressResolver;
import io.vertx.core.net.endpoint.Endpoint;
import io.vertx.core.net.endpoint.ServerEndpoint;
import io.vertx.core.net.endpoint.LoadBalancer;
import io.vertx.docgen.Source;
import io.vertx.serviceresolver.ServiceAddress;
import io.vertx.serviceresolver.ServiceResolverClient;
import io.vertx.serviceresolver.kube.KubeResolver;
import io.vertx.serviceresolver.kube.KubeResolverOptions;
import io.vertx.serviceresolver.srv.SrvResolver;
import io.vertx.serviceresolver.srv.SrvResolverOptions;

import java.util.List;

@Source
public class ServiceResolverExamples {

  public void serviceResolverClient(Vertx vertx) {

    ServiceResolverClient client = ServiceResolverClient.create(vertx, new KubeResolverOptions());

    // Resolve a service endpoint
    Future<Endpoint> fut = client.resolveEndpoint(ServiceAddress.of("the-service"));

    fut.onSuccess(endpoint -> {

      // Print physical servers details
      List<ServerEndpoint> servers = endpoint.servers();
      for (ServerEndpoint server : servers) {
        System.out.println("Available server: " + server.address());
      }
    });
  }

  public void serviceResolverClientLoadBalancing(Vertx vertx) {

    ServiceResolverClient client = ServiceResolverClient.create(
      vertx,
      LoadBalancer.POWER_OF_TWO_CHOICES,
      new KubeResolverOptions());

    // Resolve a service endpoint
    client
      .resolveEndpoint(ServiceAddress.of("the-service"))
      .onSuccess(endpoint -> {

        // Let the load balancer select an endpoint endpoint node
        // by default: round robin
        for (int i = 0; i < 10; i++) {
          ServerEndpoint node = endpoint.selectServer();
          System.out.println("Selected node: " + node.address());
        }
      });
  }

  public void configuringHttpClient(Vertx vertx, AddressResolver resolver) {

    HttpClient client = vertx.httpClientBuilder()
      .withAddressResolver(resolver)
      .build();
  }

  public void usingHttpClient(HttpClient client) {
    ServiceAddress serviceAddress = ServiceAddress.of("the-service");

    Future<HttpClientRequest> requestFuture = client.request(new RequestOptions()
      .setMethod(HttpMethod.GET)
      .setURI("/")
      .setServer(serviceAddress));

    Future<Buffer> resultFuture = requestFuture.compose(request -> request
      .send()
      .compose(response -> {
        if (response.statusCode() == 200) {
          return response.body();
        } else {
          return Future.failedFuture("Invalid status response:" + response.statusCode());
        }
      }));
  }

  public void configuringHttpClientWithLoadBalancing(Vertx vertx, AddressResolver resolver) {

    HttpClient client = vertx.httpClientBuilder()
      .withAddressResolver(resolver)
      .withLoadBalancer(LoadBalancer.LEAST_REQUESTS)
      .build();
  }

  public void usingKubernetesResolver(Vertx vertx) {

    KubeResolverOptions options = new KubeResolverOptions();

    AddressResolver resolver = KubeResolver.create(options);

    HttpClient client = vertx.httpClientBuilder()
      .withAddressResolver(resolver)
      .build();
  }

  public void configuringKubernetesResolver(String host, int port, String namespace, String bearerToken, HttpClientOptions httpClientOptions, WebSocketClientOptions wsClientOptions) {

    KubeResolverOptions options = new KubeResolverOptions()
      .setPort(port)
      .setHost(host)
      .setNamespace(namespace)
      .setBearerToken(bearerToken)
      .setHttpClientOptions(httpClientOptions)
      .setWebSocketClientOptions(wsClientOptions);
  }

  public void configuringSRVResolver(Vertx vertx, String dnsServer, int dnsPort) {

    SrvResolverOptions options = new SrvResolverOptions()
      .setPort(dnsPort)
      .setAddress(dnsServer);

    AddressResolver resolver = SrvResolver.create(options);

    HttpClient client = vertx.httpClientBuilder()
      .withAddressResolver(resolver)
      .build();
  }
}
