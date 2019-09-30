package org.folio.mod_orders

import java.io.File

import ch.qos.logback.classic.{Level, LoggerContext}
import com.typesafe.config.ConfigFactory
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.http.protocol.HttpProtocolBuilder
import org.folio.SimulationHelper
import org.slf4j.LoggerFactory
import play.api.libs.json.{JsObject, JsValue, Json}
import scalaj.http.{Http, HttpResponse}

object OrdersCrudHelper {

  val context: LoggerContext = LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext]
  context.getLogger("org.folio.mod_orders").setLevel(Level.valueOf("INFO"))

  val config = ConfigFactory.parseFile(new File(getClass.getResource("/test.conf").toURI))
  val host = config.getString("host")
  val username = config.getString("username")
  val password = config.getString("password")
  val orderSamplePath = config.getString("orderSamplePath")

  val order: HttpResponse[String] = Http(orderSamplePath).asString
  var body: JsValue = Json.parse(Json.stringify(Json.parse(order.body).as[JsObject] - "id"))

  val login: HttpResponse[String] = Http(host + "/authn/login")
    .header("Content-Type", "application/json")
    .header("x-okapi-tenant", "diku")
    .postData("{\"username\":\"" + username + "\", \"password\":\"" + password + "\"}")
    .asString

  val x_okapi_token: String = login.header("x-okapi-token").get

  val https: HttpProtocolBuilder = http
    .baseUrl(host)
    .header("Content-Type", "application/json")
    .header("x-okapi-tenant", "diku")

  val post =
    exec(http("POST Order")
    .post("/orders/composite-orders")
    .header("x-okapi-token", x_okapi_token)
    .body(StringBody(_ =>
      Json.stringify(body.as[JsObject] ++ Json.obj("poNumber" -> SimulationHelper.getRandomAlphaNumericString(10))))
    ).asJson
    .check(status is 201)
    .check(jsonPath("$..id").saveAs("id"))
  )

  val put = exec(http("PUT Order")
      .put("/orders/composite-orders/${id}")
      .header("x-okapi-token", x_okapi_token)
      .body(StringBody(_ =>
        Json.stringify(body.as[JsObject] ++ Json.obj("poNumber" -> SimulationHelper.getRandomAlphaNumericString(10))))
      ).asJson
      .check(status is 204)
    )

  val get = exec(http("GET Order")
      .get("/orders/composite-orders/${id}")
      .header("x-okapi-token", x_okapi_token)
      .check(status is 200)
    )

  val delete = exec(http("DELETE Order")
      .delete("/orders/composite-orders/${id}")
      .header("x-okapi-token", x_okapi_token)
      .check(status is 204)
    )

}
