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
package io.vertx.serviceresolver.srv;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject(generateConverter = true, publicConverter = false)
public class SrvResolverOptions {

  private String host;
  private int port;

  public SrvResolverOptions() {
  }

  public SrvResolverOptions(SrvResolverOptions other) {
    this.host = other.host;
    this.port = other.port;
  }

  public SrvResolverOptions(JsonObject json) {
    SrvLookupOptionsConverter.fromJson(json, this);
  }

  public String getHost() {
    return host;
  }

  public SrvResolverOptions setHost(String host) {
    this.host = host;
    return this;
  }

  public int getPort() {
    return port;
  }

  public SrvResolverOptions setPort(int port) {
    this.port = port;
    return this;
  }
}
