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
package io.vertx.serviceresolver.dns;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import io.vertx.serviceresolver.srv.SrvResolverOptionsConverter;

@DataObject(generateConverter = true, publicConverter = false)
public class DnsResolverOptions {

  private String host;
  private int port;

  public DnsResolverOptions() {
  }

  public DnsResolverOptions(DnsResolverOptions other) {
    this.host = other.host;
    this.port = other.port;
  }

  public DnsResolverOptions(JsonObject json) {
    DnsResolverOptionsConverter.fromJson(json, this);
  }

  public String getHost() {
    return host;
  }

  public DnsResolverOptions setHost(String host) {
    this.host = host;
    return this;
  }

  public int getPort() {
    return port;
  }

  public DnsResolverOptions setPort(int port) {
    this.port = port;
    return this;
  }
}
