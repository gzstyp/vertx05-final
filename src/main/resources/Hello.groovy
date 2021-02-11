vertx.eventBus.consumer("hello.named.addr").handler({
  message -> message.reply("hello ${message.body},使用groovy语言实现");
});