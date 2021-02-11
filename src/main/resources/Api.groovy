vertx.createHttpServer().requestHandler({ req ->
  req.response().putHeader("content-type","text/html;charset=utf-8").end("Vert.x,使用groovy语言实现")
}).listen(808);/*访问 http://127.0.0.1:808/ */