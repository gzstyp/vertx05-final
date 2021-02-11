package com.fwtai.service;

import com.fwtai.config.ConsumerAddr;
import com.fwtai.tool.ToolClient;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.CSRFHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.sstore.ClusteredSessionStore;
import io.vertx.ext.web.sstore.LocalSessionStore;
import io.vertx.ext.web.sstore.SessionStore;

/**
 * 拆分单一职责原则-web模板
 * @作者 田应平
 * @版本 v1.0
 * @创建时间 2021/2/11 1:51
 * @QQ号码 444141300
 * @Email service@yinlz.com
 * @官网 <url>http://www.yinlz.com</url>
*/
public final class WebVerticle extends AbstractVerticle {

  @Override
  public void start(final Promise<Void> start) throws Exception {
    configRouter()
      .compose(this::startHttpServer)//todo 若有参数才编写方法时写入方法,若没有参数时直接写 final Void unused 参数
      .setHandler(start::handle);
  }

  //步骤4,路由配置,todo 已抽取到 com.fwtai.service.WebVerticle,并在方法 deployOtherVerticles()引用部署;
  protected Future<Router> configRouter(){//此处没有用到参数,所以删除掉,final Void unused
    final Router router = Router.router(vertx);
    final SessionStore session1 = LocalSessionStore.create(vertx);//ok,当然也可以使用下面的方式创建!!!
    final SessionStore session2 = ClusteredSessionStore.create(vertx);//ok
    //Session
    router.route().handler(SessionHandler.create(session1));// BodyHandler.create(),支持文件上传!!!
    router.route().handler(CorsHandler.create("127.0.0.1"));
    router.route().handler(CSRFHandler.create("RjF9vTHCS2yr0zX3D50CKRiarMX+0qOpHAfcu24gWZ9bL39s48euPQniE2RhGx"));//自定义参数,高版本有2个参数,低版本只有1个参数,4.0.x版本报错
    router.get("/api/v1.0/login").handler(this::login);// http://127.0.0.1:803/api/v1.0/login
    router.get("/api/v1.0/eventBus/:name").handler(this::eventBus);// http://127.0.0.1:803/api/v1.0/eventBus/www.dwz.cloud
    router.get("/api/v1.0/getList").handler(this::getList);// http://127.0.0.1:803/api/v1.0/getList
    router.get("/api/v1.0/add").handler(this::add);// http://127.0.0.1:803/api/v1.0/add?name=zjy&password=886&age=37
    router.get("/api/v1.0/getById/:kid").handler(this::getById);// http://127.0.0.1:803/api/v1.0/getById/1
    router.route().handler(StaticHandler.create("web"));//指定root根目录,默认访问路径: http://192.168.3.108:803/
    return Promise.succeededPromise(router).future();//todo 新版本会报错
  }

  //步骤5,有参数,todo 已抽取到 com.fwtai.service.WebVerticle,并在方法 deployOtherVerticles()引用部署;
  protected Future<Void> startHttpServer(final Router router){
    final JsonObject http = config().getJsonObject("http");// {"port":803}
    final Integer httpPort = http.getInteger("port",801);
    final HttpServer server = vertx.createHttpServer().requestHandler(router);
    return Future.<HttpServer>future(promise -> server.listen(httpPort,promise)).mapEmpty();//.mapEmpty() 终止符
  }

  //方法的参数类型,blockingHandler(Handler<RoutingContext> requestHandler)
  protected void login(final RoutingContext context){
    ToolClient.getResponse(context).end("欢迎登录,使用java实现");
  }

  //方法的参数类型,blockingHandler(Handler<RoutingContext> requestHandler)
  protected void eventBus(final RoutingContext context){
    final String name = context.pathParam("name");
    vertx.eventBus().request(ConsumerAddr.addr_named,name,msg->{
      ToolClient.getResponse(context).end("EventBus_java,"+msg.result().body());
    });
  }

  //有参数调用
  protected void add(final RoutingContext context){
    final JsonObject params = ToolClient.getParamsJson(context);//传入参数
    vertx.eventBus().request(ConsumerAddr.add_addr,params,reply->{//reply是响应数据|应答数据|回复数据
      ToolClient.getResponse(context).end("异步,非阻塞,消息驱动,"+reply.result().body());
    });
  }

  protected void getById(final RoutingContext context){
    final String kid = context.request().getParam("kid");
    vertx.eventBus().request(ConsumerAddr.byId_addr,kid,reply->{//reply是响应数据|应答数据|回复数据
      ToolClient.getResponse(context).end("消息驱动,非阻塞,"+reply.result().body());
    });
  }

  //无参数调用
  protected void getList(final RoutingContext context){
    final String kid = context.request().getParam("kid");
    vertx.eventBus().request(ConsumerAddr.list_addr,kid,reply->{
      ToolClient.getResponse(context).end("addr_EventBus_java,"+reply.result().body());
    });
  }
}