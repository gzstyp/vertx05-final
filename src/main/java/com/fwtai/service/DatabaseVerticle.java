package com.fwtai.service;

import com.fwtai.config.ConsumerAddr;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLClient;
import io.vertx.ext.sql.SQLConnection;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;

/**
 * 拆分单一职责原则-数据库访问模板
 * @作者 田应平
 * @版本 v1.0
 * @创建时间 2021/2/11 1:51
 * @QQ号码 444141300
 * @Email service@yinlz.com
 * @官网 <url>http://www.yinlz.com</url>
*/
public final class DatabaseVerticle extends AbstractVerticle {

  private SQLClient client;

  protected final String sql_list = "SELECT kid,name,password,age from user";
  protected final String sql_map = "SELECT kid,name,password,age from user WHERE kid = 1";
  protected final String sql_add = "INSERT INTO user (name,password,age) VALUES (?,?,?)";
  protected final String sql_edit = "UPDATE user SET password = ? WHERE kid = ? ";

  @Override
  public void start(final Promise<Void> start) throws Exception {
    doDatabaseMigrations()
      .compose(this::configSQLClient)//todo,若有参数才编写方法时写入方法,若没有参数时直接写 final Void unused 参数[因为不是最后一个链式调用,所以得有无返回值的参数???为什么有些要参数,有些不需要?]
      .compose(this::configEventBusConsumer)
      .setHandler(start::handle);
  }

  protected Future<Void> configEventBusConsumer(final Void unused){
    vertx.eventBus().consumer(ConsumerAddr.list_addr).handler(this::listAll);
    vertx.eventBus().consumer(ConsumerAddr.byId_addr).handler(this::getById);
    vertx.eventBus().consumer(ConsumerAddr.edit_addr).handler(this::edit);
    vertx.eventBus().consumer(ConsumerAddr.add_addr).handler(this::add);
    return Promise.<Void> succeededPromise().future();
  }

  //因为不是最后一个链式调用,所以得有无返回值的参数
  protected Future<Void> configSQLClient(final Void unused){
    client = JDBCClient.createShared(vertx,config().getJsonObject("db"));
    return Promise.<Void> succeededPromise().future();//todo
  }

  //查询集合列表
  protected Future<ResultSet> queryWithPatamters(final SQLConnection connection,final String sql,final JsonArray params){
    return Future.<ResultSet> future(promise -> connection.queryWithParams(sql,params,promise));
  }

  //查询单条数据
  protected Future<JsonObject> queryMap(final ResultSet rs){
    if(rs.getNumRows() >= 1){
      return Promise.succeededPromise(rs.getRows().get(0)).future();
    }else{
      return Promise.<JsonObject> failedPromise("暂无数据").future();
    }
  }

  //查询集合列表
  protected Future<JsonArray> queryList(final ResultSet rs){
    return Promise.succeededPromise(new JsonArray(rs.getRows())).future();
  }

  protected void listAll(final Message<Object> msg){
    Future.<SQLConnection> future(client::getConnection)
      .compose(conn -> this.queryWithPatamters(conn,sql_list,new JsonArray()))
      .compose(this::queryList)
      .setHandler(res ->{
        if(res.succeeded()){
          msg.reply(res.result());
        }else{
          msg.fail(500,res.cause().getLocalizedMessage());
        }
      });
  }

  protected void getById(final Message<Object> msg){
    if(msg.body() instanceof String){
      final String id = (String) msg.body();
      final JsonArray params = new JsonArray().add(id);//todo 支持链式调用
      Future.<SQLConnection>future(client::getConnection)
        .compose(connection -> this.queryWithPatamters(connection,sql_map,params))
        .compose(this::queryMap)
        .setHandler(res ->{
          if(res.succeeded()){
            msg.reply(res.result());
          }else{
            msg.fail(500,res.cause().getLocalizedMessage());
          }
        });
    }else{
      msg.fail(202,"参数不完整1");
      //msg.fail(400,"参数不完整");
    }
  }

  //添加操作
  protected void add(final Message<Object> msg){
    if(msg.body() instanceof JsonObject){
      final JsonObject json = (JsonObject) msg.body();
      if(json.containsKey("name")){
        final JsonArray params = new JsonArray()
          .add(json.getString("name"))
          .add(json.getString("password","000000"))
          .add(json.getString("age"));
        Future.<SQLConnection>future(client::getConnection)
          .compose(connection -> this.queryWithPatamters(connection,sql_add,params))
          .compose(this::queryMap)
          .setHandler(res ->{
            if(res.succeeded()){
              msg.reply(res.result());
            }else{
              msg.fail(500,"系统出现错误");
            }
          });
      }
    }else{
      msg.fail(400,"参数不完整2");
    }
  }

  //添加操作
  protected void edit(final Message<Object> msg){
    if(msg.body() instanceof JsonObject){
      final JsonObject json = (JsonObject) msg.body();
      final JsonArray params = new JsonArray()
        .add(json.getString("password"));
      Future.<SQLConnection>future(client::getConnection)
        .compose(connection -> this.queryWithPatamters(connection,sql_edit,params))
        .compose(this::queryMap)
        .setHandler(res ->{
          if(res.succeeded()){
            msg.reply(res.result());
          }else{
            msg.fail(500,"系统出现错误");
          }
        });
    }else{
      msg.fail(400,"参数不完整3");
    }
  }

  protected Future<Void> doDatabaseMigrations(){
    final JsonObject jsonObject = config().getJsonObject("db");
    final String url = jsonObject.getString("url","jdbc:mysql://192.168.3.66:3306/vertx04-final?useUnicode=true&characterEncoding=utf-8&useSSL=false&allowMultiQueries=true&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true");
    final String user = jsonObject.getString("user","root");
    final String password = jsonObject.getString("password","rootFwtai");
    final Flyway flyway = Flyway.configure().dataSource(url,user,password).load();
    final Promise<Void> promise = Promise.promise();
    try {
      flyway.migrate();
      promise.complete();
    } catch (final FlywayException fe) {
      promise.fail(fe);
    }
    return promise.future();
  }
}