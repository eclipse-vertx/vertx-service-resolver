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
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.net.AddressResolver;
import io.vertx.serviceresolver.ServiceAddress;

public class ServiceLookupExamples {

  public void httpClient(Vertx vertx) {

    AddressResolver resolver = null;

    HttpClient client = vertx.httpClientBuilder()
      .withAddressResolver(resolver)
      .build();

    Future<HttpClientRequest> requestFuture = client.request(new RequestOptions()
      .setMethod(HttpMethod.GET)
      .setURI("/")
      .setServer(ServiceAddress.create("the-service")));

    Future<Buffer> bodyFuture = requestFuture.compose(request -> request
      .send()
      .compose(response -> {
        if (response.statusCode() == 200) {
          return response.body();
        } else {
          return Future.failedFuture("Invalid status response:" + response.statusCode());
        }
      }));
  }

}
