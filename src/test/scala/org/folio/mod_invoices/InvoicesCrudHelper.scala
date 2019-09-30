package org.folio.mod_invoices

import java.io.File

import com.typesafe.config.ConfigFactory
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.http.protocol.HttpProtocolBuilder
import org.folio.SimulationHelper
import play.api.libs.json.{JsObject, JsValue, Json}
import scalaj.http.{Http, HttpResponse}

object InvoicesCrudHelper {

  val config = ConfigFactory.parseFile(new File(getClass.getResource("/test.conf").toURI))
  val host = config.getString("host")
  val username = config.getString("username")
  val password = config.getString("password")
  val invoiceSamplePath = config.getString("invoiceSamplePath")

  val invoice: HttpResponse[String] = Http(invoiceSamplePath).asString
  var body: JsValue = Json.parse(Json.stringify(Json.parse(invoice.body).as[JsObject] - "id"))

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
    exec(http("POST Invoice")
    .post("/invoice/invoices")
    .header("x-okapi-token", x_okapi_token)
    .body(StringBody(_ =>
      Json.stringify(body.as[JsObject] ++ Json.obj("folioInvoiceNo" -> SimulationHelper.getRandomAlphaNumericString(10))))
    ).asJson
    .check(status is 201)
    .check(jsonPath("$..id").saveAs("id"))
  )

  val put = exec(http("PUT Invoice")
      .put("/invoice/invoices/${id}")
      .header("x-okapi-token", x_okapi_token)
      .body(StringBody(_ =>
        Json.stringify(body.as[JsObject] ++ Json.obj("folioInvoiceNo" -> SimulationHelper.getRandomAlphaNumericString(10))))
      ).asJson
      .check(status is 204)
    )

  val get = exec(http("GET Invoice")
      .get("/invoice/invoices/${id}")
      .header("x-okapi-token", x_okapi_token)
      .check(status is 200)
    )

  val delete = exec(http("DELETE Invoice")
      .delete("/invoice/invoices/${id}")
      .header("x-okapi-token", x_okapi_token)
      .check(status is 204)
    )

}
