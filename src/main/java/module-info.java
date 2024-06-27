module io.vertx.serviceresolver {
  requires transitive io.vertx.core;
  requires static io.vertx.codegen.api;
  requires static io.vertx.codegen.json;
  requires static vertx.docgen;
  exports io.vertx.serviceresolver;
  exports io.vertx.serviceresolver.kube;
  exports io.vertx.serviceresolver.srv;
}
