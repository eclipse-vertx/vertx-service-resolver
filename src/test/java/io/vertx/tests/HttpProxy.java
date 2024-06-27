package io.vertx.tests;

import io.vertx.core.Vertx;
import io.vertx.core.http.*;
import io.vertx.core.net.JksOptions;
import io.vertx.core.net.SocketAddress;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class HttpProxy {

  private final Vertx vertx;
  private HttpServer server;
  private HttpClient httpClient;
  private WebSocketClient wsClient;
  private SocketAddress origin;
  private int port;
  private final Set<WebSocketBase> webSockets = ConcurrentHashMap.newKeySet();

  public HttpProxy(Vertx vertx) {
    this.vertx = vertx;
    this.httpClient = vertx.createHttpClient();
    this.wsClient = vertx.createWebSocketClient();
  }

  public HttpProxy origin(SocketAddress origin) {
    this.origin = origin;
    return this;
  }

  public HttpProxy port(int port) {
    this.port = port;
    return this;
  }

  public void start() throws Exception {
    HttpServer server = vertx.createHttpServer(new HttpServerOptions()
      .setSsl(true)
      .setKeyCertOptions(new JksOptions()
        .setPath("server-keystore.jks")
        .setPassword("wibble"))
    );
    server.requestHandler(this::handleRequest);
    server
      .listen(port)
      .toCompletionStage()
      .toCompletableFuture()
      .get(20, TimeUnit.SECONDS);
  }

  private void handleRequest(HttpServerRequest serverRequest) {
    if (serverRequest.getHeader("upgrade") != null) {
      WebSocketConnectOptions options = new WebSocketConnectOptions();
      options.setServer(origin);
      options.setURI(serverRequest.uri());
      serverRequest.pause();
      wsClient.connect(options).onComplete(ar -> {
        if (ar.succeeded()) {
          WebSocket wsc = ar.result();
          AtomicBoolean closed = new AtomicBoolean();
          wsc.closeHandler(v -> {
            closed.set(true);
          });
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
        .setServer(origin)
        .setMethod(serverRequest.method())
        .setURI(serverRequest.uri());
      serverRequest.body().onComplete(ar_ -> {
        if (ar_.succeeded()) {
          httpClient.request(options).onComplete(ar -> {
            if (ar.succeeded()) {
              HttpClientRequest clientRequest = ar.result();
              clientRequest.send(ar_.result()).onComplete(ar2 -> {
                if (ar2.succeeded()) {
                  HttpClientResponse clientResponse = ar2.result();
                  HttpServerResponse serverResponse = serverRequest.response();
                  serverResponse.putHeader(HttpHeaders.CONTENT_LENGTH, clientResponse.getHeader(HttpHeaders.CONTENT_LENGTH));
                  clientResponse.pipeTo(serverResponse);
                } else {
                  serverRequest.response().setStatusCode(500).end();
                }
              });
            } else {
              ar.cause().printStackTrace();
              serverRequest.response().reset();
            }
          });
        } else {
          // Nothing to do ? (compose?)
        }
      });
    }
  }

  public Set<WebSocketBase> webSockets() {
    return webSockets;
  }
}
