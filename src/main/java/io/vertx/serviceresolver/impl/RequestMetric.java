package io.vertx.serviceresolver.impl;

public class RequestMetric<E> {

  final EndpointImpl<E> endpoint;
  long requestBegin;
  long requestEnd;
  long responseBegin;
  long responseEnd;
  Throwable failure;

  public RequestMetric(EndpointImpl<E> endpoint) {
    this.endpoint = endpoint;
  }
}
