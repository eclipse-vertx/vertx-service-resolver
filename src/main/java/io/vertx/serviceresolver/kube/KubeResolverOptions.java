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
package io.vertx.serviceresolver.kube;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.WebSocketClientOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.core.net.SocketAddress;
import io.vertx.serviceresolver.ServiceResolverOptions;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 *
 */
@DataObject
@JsonGen(publicConverter = false)
public class KubeResolverOptions extends ServiceResolverOptions {

  private static final String KUBERNETES_SERVICE_HOST = "KUBERNETES_SERVICE_HOST";
  private static final String KUBERNETES_SERVICE_PORT = "KUBERNETES_SERVICE_PORT";
  private static final String KUBERNETES_SERVICE_ACCOUNT_TOKEN = "/var/run/secrets/kubernetes.io/serviceaccount/token";
  private static final String KUBERNETES_SERVICE_ACCOUNT_CA = "/var/run/secrets/kubernetes.io/serviceaccount/ca.crt";
  private static final String KUBERNETES_SERVICE_ACCOUNT_NAMESPACE = "/var/run/secrets/kubernetes.io/serviceaccount/namespace";

  private static final SocketAddress DEFAULT_SERVER;
  private static final String DEFAULT_TOKEN;
  private static final String DEFAULT_NAMESPACE;
  private static final HttpClientOptions DEFAULT_HTTP_CLIENT_OPTIONS;
  private static final WebSocketClientOptions DEFAULT_WEB_SOCKET_OPTIONS;

  static {
    String host = System.getenv(KUBERNETES_SERVICE_HOST);
    Integer port = 443;
    String v = System.getenv(KUBERNETES_SERVICE_PORT);
    if (v != null) {
      try {
        port = Integer.parseInt(v);
      } catch (NumberFormatException ignore) {
      }
    }
    File tokenFile = new File(KUBERNETES_SERVICE_ACCOUNT_TOKEN);
    String token = null;
    if (tokenFile.exists()) {
      try {
        token = Buffer.buffer(Files.readAllBytes(tokenFile.toPath())).toString();
      } catch (IOException ignore) {
      }
    }
    String namespace = "default";
    File namespaceFile = new File(KUBERNETES_SERVICE_ACCOUNT_NAMESPACE);
    if (namespaceFile.exists()) {
      try {
        namespace = Buffer.buffer(Files.readAllBytes(namespaceFile.toPath())).toString();
      } catch (IOException ignore) {
      }
    }
    HttpClientOptions httpClientOptions = new HttpClientOptions().setSsl(true);
    WebSocketClientOptions webSocketClientOptions = new WebSocketClientOptions().setSsl(true);
    File caFile = new File(KUBERNETES_SERVICE_ACCOUNT_CA);
    if (caFile.exists()) {
      PemTrustOptions pemTrustOptions = new PemTrustOptions().addCertPath(KUBERNETES_SERVICE_ACCOUNT_CA);
      httpClientOptions.setTrustOptions(pemTrustOptions);
      webSocketClientOptions.setTrustOptions(pemTrustOptions);
    }
    DEFAULT_SERVER = host != null ? SocketAddress.inetSocketAddress(port, host) : null;
    DEFAULT_TOKEN = token;
    DEFAULT_NAMESPACE = namespace;
    DEFAULT_HTTP_CLIENT_OPTIONS = httpClientOptions;
    DEFAULT_WEB_SOCKET_OPTIONS = webSocketClientOptions;
  }

  private SocketAddress server = DEFAULT_SERVER;
  private String namespace = DEFAULT_NAMESPACE;
  private String bearerToken = DEFAULT_TOKEN;
  private HttpClientOptions httpClientOptions = new HttpClientOptions(DEFAULT_HTTP_CLIENT_OPTIONS);
  private WebSocketClientOptions webSocketClientOptions = new WebSocketClientOptions(DEFAULT_WEB_SOCKET_OPTIONS);

  /**
   * Constructor with default options, those might have been set from the pod environment when running in a pod.
   */
  public KubeResolverOptions() {
    server = DEFAULT_SERVER;
    namespace = DEFAULT_NAMESPACE;
    bearerToken = DEFAULT_TOKEN;
    httpClientOptions = new HttpClientOptions(DEFAULT_HTTP_CLIENT_OPTIONS);
    webSocketClientOptions = new WebSocketClientOptions(DEFAULT_WEB_SOCKET_OPTIONS);
  }

  /**
   * Default constructor.
   */
  public KubeResolverOptions(KubeResolverOptions other) {
    this.server = other.server;
    this.namespace = other.namespace;
    this.bearerToken = other.bearerToken;
    this.httpClientOptions = other.httpClientOptions != null ? new HttpClientOptions(other.httpClientOptions) : new HttpClientOptions();
    this.webSocketClientOptions = other.webSocketClientOptions != null ? new WebSocketClientOptions(other.webSocketClientOptions) : new WebSocketClientOptions();
  }

  /**
   * JSON constructor
   */
  public KubeResolverOptions(JsonObject json) {
    KubeResolverOptionsConverter.fromJson(json, this);
  }

  /**
   * @return the Kubernetes server address
   */
  public SocketAddress getServer() {
    return server;
  }

  /**
   * Set the Kubernetes server address
   *
   * @param server the address
   * @return this options instance
   */
  public KubeResolverOptions setServer(SocketAddress server) {
    this.server = server;
    return this;
  }

  public String getNamespace() {
    return namespace;
  }

  public KubeResolverOptions setNamespace(String namespace) {
    this.namespace = namespace;
    return this;
  }

  public String getBearerToken() {
    return bearerToken;
  }

  public KubeResolverOptions setBearerToken(String bearerToken) {
    this.bearerToken = bearerToken;
    return this;
  }

  public HttpClientOptions getHttpClientOptions() {
    return httpClientOptions;
  }

  public KubeResolverOptions setHttpClientOptions(HttpClientOptions httpClientOptions) {
    this.httpClientOptions = httpClientOptions;
    return this;
  }

  public WebSocketClientOptions getWebSocketClientOptions() {
    return webSocketClientOptions;
  }

  public KubeResolverOptions setWebSocketClientOptions(WebSocketClientOptions webSocketClientOptions) {
    this.webSocketClientOptions = webSocketClientOptions;
    return this;
  }
}
