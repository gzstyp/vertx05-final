vertx.createHttpServer().requestHandler(function (req) {
    req.response().putHeader("content-type","text/html;charset=utf-8").end("Vert.x,使用javascript语言实现")
}).listen(909);/*访问 http://127.0.0.1:909/ */