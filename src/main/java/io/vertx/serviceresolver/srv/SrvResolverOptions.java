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
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;
import io.vertx.serviceresolver.ServiceResolverOptions;

import java.util.concurrent.TimeUnit;

@DataObject
@JsonGen(publicConverter = false)
public class SrvResolverOptions extends ServiceResolverOptions {

  public static final int MIN_TTL = 0;
  public static final TimeUnit MIN_TTL_UNIT = TimeUnit.SECONDS;

  private String host;
  private int port;
  private int minTTL = MIN_TTL;
  private TimeUnit minTTLUnit = TimeUnit.SECONDS;

  public SrvResolverOptions() {
  }

  public SrvResolverOptions(SrvResolverOptions other) {
    this.host = other.host;
    this.port = other.port;
    this.minTTL = other.minTTL;
  }

  public SrvResolverOptions(JsonObject json) {
    SrvResolverOptionsConverter.fromJson(json, this);
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

  /**
   * @return the minimum amount of time in {@link #setMinTTLUnit(TimeUnit) unit of time} the resolver caches DNS responses.
   */
  public int getMinTTL() {
    return minTTL;
  }

  /**
   * <p>Set the minimum amount of time in {@link #setMinTTLUnit(TimeUnit) unit of time} the resolver caches DNS responses,
   * the default value is {@code 0}</p>
   *
   * <p>This overrides the DNS packet TTL when the value is too small or not cached to ensure caching occurs in the resolver.</p>
   *
   * @param minTTL the minimum amount of time the resolver caches DNS responses
   * @return this options instance
   */
  public SrvResolverOptions setMinTTL(int minTTL) {
    if (minTTL < 0) {
      throw new IllegalArgumentException("Min TTL (" + minTTL + ") must be > 0");
    }
    this.minTTL = minTTL;
    return this;
  }

  /**
   * @return the unit of {@link #setMinTTL(int)}.
   */
  public TimeUnit getMinTTLUnit() {
    return minTTLUnit;
  }

  /**
   * Set the unit of {@link #setMinTTL(int)}, the default value is {@link TimeUnit#SECONDS}.
   *
   * @param minTTLUnit the unit
   * @return this options instance
   */
  public SrvResolverOptions setMinTTLUnit(TimeUnit minTTLUnit) {
    this.minTTLUnit = minTTLUnit;
    return this;
  }
}
