package com.fwtai.service;

import com.fwtai.config.ConsumerAddr;
import io.vertx.core.AbstractVerticle;

/**
 * 消费者|消费端
 * @作者 田应平
 * @版本 v1.0
 * @创建时间 2021-02-08 9:36
 * @QQ号码 444141300
 * @Email service@dwlai.com
 * @官网 http://www.fwtai.com
*/
public final class VertxEventBus extends AbstractVerticle{

  @Override
  public void start(){
    vertx.eventBus().consumer(ConsumerAddr.addr_vertx,message->{
      message.reply("hello vert.x world");//用于 Hello.js 调用或 com.fwtai.service.VertxRouter 的方法eventBus()调用
    });
    vertx.eventBus().consumer(ConsumerAddr.addr_named,message->{
      final String name = (String) message.body(); //用于 Hello.groovy 调用 或 com.fwtai.service.VertxRouter的方法 eventBusName() 调用
      message.reply(name);
    });
  }
}