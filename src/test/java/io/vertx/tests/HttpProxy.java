package io.vertx.tests;

import io.vertx.core.Vertx;
import io.vertx.core.http.*;
import io.vertx.core.net.KeyCertOptions;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.net.TrustOptions;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

public class HttpProxy {

  private final Vertx vertx;
  private HttpServer server;
  private HttpClient httpClient;
  private WebSocketClient webSocketClient;
  private final Set<WebSocketBase> webSockets = ConcurrentHashMap.newKeySet();
  private HttpClientOptions httpConfig = new HttpClientOptions().setTrustAll(true);
  private WebSocketClientOptions webSocketConfig = new WebSocketClientOptions().setTrustAll(true);
  private int port;
  private Predicate<HttpServerRequest> requestHandler = req -> true;

  public HttpProxy(Vertx vertx) {
    this.vertx = vertx;
  }

  public HttpProxy requestHandler(Predicate<HttpServerRequest> requestHandler) {
    this.requestHandler = requestHandler == null ? req -> true : requestHandler;
    return this;
  }

  public HttpProxy origin(SocketAddress origin) {
    httpConfig.setDefaultPort(origin.port());
    httpConfig.setDefaultHost(origin.host());
    webSocketConfig.setDefaultPort(origin.port());
    webSocketConfig.setDefaultHost(origin.host());
    return this;
  }

  public HttpProxy port(int port) {
    this.port = port;
    return this;
  }

  public HttpProxy keyCertOptions(KeyCertOptions keyCertOptions) {
    httpConfig.setKeyCertOptions(keyCertOptions.copy());
    webSocketConfig.setKeyCertOptions(keyCertOptions.copy());
    return this;
  }

  public HttpProxy trustOptions(TrustOptions trustOptions) {
    httpConfig.setTrustOptions(trustOptions.copy());
    webSocketConfig.setTrustOptions(trustOptions.copy());
    return this;
  }

  public HttpProxy protocol(HttpVersion version) {
    httpConfig.setProtocolVersion(version);
    return this;
  }

  public HttpProxy ssl(boolean ssl) {
    httpConfig.setSsl(ssl);
    webSocketConfig.setSsl(ssl);
    return this;
  }

  public void start() throws Exception {
    this.httpClient = vertx.createHttpClient(httpConfig);
    this.webSocketClient = vertx.createWebSocketClient(webSocketConfig);
    server = vertx.createHttpServer();
    server.requestHandler(this::handleRequest);
    server
      .listen(port)
      .toCompletionStage()
      .toCompletableFuture()
      .get(20, TimeUnit.SECONDS);
  }

  private void handleRequest(HttpServerRequest serverRequest) {
    Predicate<HttpServerRequest> handler = requestHandler;
    if (!handler.test(serverRequest)) {
      return;
    }
    if (serverRequest.canUpgradeToWebSocket()) {
      WebSocketConnectOptions options = new WebSocketConnectOptions();
      options.setURI(serverRequest.uri());
      serverRequest.pause();
      webSocketClient.connect(options).onComplete(ar -> {
        if (ar.succeeded()) {
          WebSocket wsc = ar.result();
          AtomicBoolean closed = new AtomicBoolean();
          wsc.closeHandler(v -> {
            closed.set(true);
          });
          wsc.pause();
          serverRequest.toWebSocket().onComplete(ar2 -> {
            if (!closed.get()) {
              if (ar2.succeeded()) {
                ServerWebSocket wss = ar2.result();
                wsc.handler(wss::write);
                wss.endHandler(v -> {
                  wsc.end();
                });
                wsc.endHandler(v -> {
                  wss.end();
                });
                wss.closeHandler(v -> {
                  wsc.close();
                });
                wsc.closeHandler(v -> {
                  webSockets.remove(wss);
                  wss.close();
                });
                webSockets.add(wss);
                wsc.resume();
              } else {
                wsc.close();
              }
            } else {
              if (ar2.succeeded()) {
                ar2.result().close();
              }
            }
          });
        } else {
          serverRequest.response().setStatusCode(500).end();
        }
      });
    } else {
      RequestOptions options = new RequestOptions()
        .setMethod(serverRequest.method())
        .setURI(serverRequest.uri());
      serverRequest.body().onSuccess(requestBody -> {
        httpClient
          .request(options)
          .compose(clientRequest -> clientRequest
            .send(requestBody)
            .compose(clientResponse -> {
              serverRequest.response().setStatusCode(clientResponse.statusCode());
              return clientResponse.body();
            }))
          .onComplete(ar -> {
            if (ar.succeeded()) {
              serverRequest.response().end(ar.result());
            } else {
              serverRequest.response().setStatusCode(500).end();
            }
          });
      });
    }
  }

  public Set<WebSocketBase> webSockets() {
    return webSockets;
  }

  public void close() {
    webSocketClient.close().await();
    httpClient.close().await();
    server.close().await();
  }
}
