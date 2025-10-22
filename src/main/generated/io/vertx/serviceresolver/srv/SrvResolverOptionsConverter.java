package io.vertx.serviceresolver.srv;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

/**
 * Converter and mapper for {@link io.vertx.serviceresolver.srv.SrvResolverOptions}.
 * NOTE: This class has been automatically generated from the {@link io.vertx.serviceresolver.srv.SrvResolverOptions} original class using Vert.x codegen.
 */
public class SrvResolverOptionsConverter {

   static void fromJson(Iterable<java.util.Map.Entry<String, Object>> json, SrvResolverOptions obj) {
    for (java.util.Map.Entry<String, Object> member : json) {
      switch (member.getKey()) {
        case "server":
          if (member.getValue() instanceof JsonObject) {
            obj.setServer(io.vertx.core.net.SocketAddress.fromJson((JsonObject)member.getValue()));
          }
          break;
        case "minTTL":
          if (member.getValue() instanceof Number) {
            obj.setMinTTL(((Number)member.getValue()).intValue());
          }
          break;
        case "minTTLUnit":
          if (member.getValue() instanceof String) {
            obj.setMinTTLUnit(java.util.concurrent.TimeUnit.valueOf((String)member.getValue()));
          }
          break;
      }
    }
  }

   static void toJson(SrvResolverOptions obj, JsonObject json) {
    toJson(obj, json.getMap());
  }

   static void toJson(SrvResolverOptions obj, java.util.Map<String, Object> json) {
    if (obj.getServer() != null) {
      json.put("server", obj.getServer().toJson());
    }
    json.put("minTTL", obj.getMinTTL());
    if (obj.getMinTTLUnit() != null) {
      json.put("minTTLUnit", obj.getMinTTLUnit().name());
    }
  }
}
