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
package io.vertx.serviceresolver;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.net.Address;

import java.util.Objects;

/**
 * A general purpose service address, defined by a {@link #name()}.
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
@DataObject
public interface ServiceAddress extends Address {

  /**
   * Create a service address
   *
   * @param name the service name
   * @return the service address
   */
  public static ServiceAddress of(String name) {
    Objects.requireNonNull(name);
    return new ServiceAddress() {
      @Override
      public String name() {
        return name;
      }
      @Override
      public boolean equals(Object obj) {
        if (obj == this) {
          return true;
        }
        if (obj instanceof ServiceAddress) {
          ServiceAddress that = (ServiceAddress) obj;
          return name.equals(that.name());
        }
        return super.equals(obj);
      }
      @Override
      public int hashCode() {
        return name.hashCode();
      }
      @Override
      public String toString() {
        return name;
      }
    };
  }

  String name();

}
