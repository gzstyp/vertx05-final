package com.fwtai.service;

import com.fwtai.tool.ToolClient;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.ext.web.Router;

public final class UserVerticle extends AbstractVerticle {

  @Override
  public void start(final Promise<Void> startPromise) throws Exception {
    final Router router = Router.router(vertx);
    // http://127.0.0.1:602/api/user
    router.get("/api/user").handler(context -> {
      ToolClient.getResponse(context).end("欢迎使用vertx接口api");
    });
    vertx.createHttpServer().requestHandler(router).listen(602);
    startPromise.complete();
  }
}