package io.vertx.serviceresolver.kube;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

/**
 * Converter and mapper for {@link io.vertx.serviceresolver.kube.KubeResolverOptions}.
 * NOTE: This class has been automatically generated from the {@link io.vertx.serviceresolver.kube.KubeResolverOptions} original class using Vert.x codegen.
 */
public class KubeResolverOptionsConverter {

   static void fromJson(Iterable<java.util.Map.Entry<String, Object>> json, KubeResolverOptions obj) {
    for (java.util.Map.Entry<String, Object> member : json) {
      switch (member.getKey()) {
        case "host":
          if (member.getValue() instanceof String) {
            obj.setHost((String)member.getValue());
          }
          break;
        case "port":
          if (member.getValue() instanceof Number) {
            obj.setPort(((Number)member.getValue()).intValue());
          }
          break;
        case "namespace":
          if (member.getValue() instanceof String) {
            obj.setNamespace((String)member.getValue());
          }
          break;
        case "bearerToken":
          if (member.getValue() instanceof String) {
            obj.setBearerToken((String)member.getValue());
          }
          break;
        case "httpClientOptions":
          if (member.getValue() instanceof JsonObject) {
            obj.setHttpClientOptions(new io.vertx.core.http.HttpClientOptions((io.vertx.core.json.JsonObject)member.getValue()));
          }
          break;
        case "webSocketClientOptions":
          if (member.getValue() instanceof JsonObject) {
            obj.setWebSocketClientOptions(new io.vertx.core.http.WebSocketClientOptions((io.vertx.core.json.JsonObject)member.getValue()));
          }
          break;
      }
    }
  }

   static void toJson(KubeResolverOptions obj, JsonObject json) {
    toJson(obj, json.getMap());
  }

   static void toJson(KubeResolverOptions obj, java.util.Map<String, Object> json) {
    if (obj.getHost() != null) {
      json.put("host", obj.getHost());
    }
    json.put("port", obj.getPort());
    if (obj.getNamespace() != null) {
      json.put("namespace", obj.getNamespace());
    }
    if (obj.getBearerToken() != null) {
      json.put("bearerToken", obj.getBearerToken());
    }
    if (obj.getHttpClientOptions() != null) {
      json.put("httpClientOptions", obj.getHttpClientOptions().toJson());
    }
    if (obj.getWebSocketClientOptions() != null) {
      json.put("webSocketClientOptions", obj.getWebSocketClientOptions().toJson());
    }
  }
}
