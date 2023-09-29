package io.vertx.serviceresolver.impl;

public class RequestMetric<E> {

  final EndpointImpl<E> endpoint;
  long requestBegin, responseBegin;

  public RequestMetric(EndpointImpl<E> endpoint) {
    this.endpoint = endpoint;
  }
}
