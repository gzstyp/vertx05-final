vertx.eventBus().consumer("hello.vertx.addr",function(message){
  message.reply("hello vert.x world,使用javascript语言实现");//用于 Hello.js 调用或 com.fwtai.example.VertxRouter 的方法eventBus()调用
});