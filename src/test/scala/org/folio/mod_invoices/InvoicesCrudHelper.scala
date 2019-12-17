package org.folio.mod_invoices

import java.io.File

import com.typesafe.config.ConfigFactory
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.http.protocol.HttpProtocolBuilder
import org.folio.SimulationHelper
import play.api.libs.json._
import scalaj.http.{Http, HttpResponse}

object InvoicesCrudHelper {

  val config = ConfigFactory.parseFile(new File(getClass.getResource("/test.conf").toURI))
  val host = config.getString("host")
  val username = config.getString("username")
  val password = config.getString("password")
  val invoiceSamplePath = config.getString("invoiceSamplePath")
  val invoiceLineSamplePath = config.getString("invoiceLineSamplePath")
  val documentSamplePath = config.getString("documentSamplePath")

  var invoiceId = ""

  // Transformers
  val metadata_id_remover =  (__ \ "documentMetadata" \ "id" ).json.prune

  def invoiceIdAdder(invId: String) : Reads[JsObject] = {
    (__ \ "documentMetadata" ).json.update(__.read[JsObject].map{ o => o ++ Json.obj("invoiceId" -> invId)})
  }


  val invoice: HttpResponse[String] = Http(invoiceSamplePath).asString
  var invoice_body = Json.parse(Json.stringify(Json.parse(invoice.body).as[JsObject] - "id"))

  val invoice_line: HttpResponse[String] = Http(invoiceLineSamplePath).asString
  var invoice_line_body: JsValue = Json.parse(Json.stringify(Json.parse(invoice_line.body).as[JsObject] - "id"))

  val document: HttpResponse[String] = Http(documentSamplePath).asString

  var document_body = Json.parse(document.body).transform(metadata_id_remover).get


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

  // ----------------------------------- POST Invoice, Invoice-Line, Document -----------------------------------

  val post_invoice =
    exec(http("POST Invoice")
    .post("/invoice/invoices")
    .header("x-okapi-token", x_okapi_token)
    .body(StringBody(_ =>
      Json.stringify(invoice_body.as[JsObject] ++ Json.obj("folioInvoiceNo" -> SimulationHelper.getRandomAlphaNumericString(10))))
    ).asJson
    .check(status is 201)
    .check(jsonPath("$..id").saveAs("invoiceId"))
  ).exec(session => {
      session.set("invoiceId", invoiceId)
      session
    })

  val post_invoice_line =
    exec(http("POST Invoice Line")
      .post("/invoice/invoice-lines")
      .header("x-okapi-token", x_okapi_token)
      .body(StringBody(session =>
        Json.stringify(invoice_line_body.as[JsObject] ++ Json.obj("invoiceId" -> (session("invoiceId").as[String]))))
      ).asJson
      .check(status is 201)
      .check(jsonPath("$..id").saveAs("invoiceLineId"))
    )

  val post_invoice_document =
    exec(http("POST Invoice Document")
      .post("/invoice/invoices/${invoiceId}/documents")
      .header("x-okapi-token", x_okapi_token)
      .body(StringBody(session =>
        Json.stringify(document_body.transform(invoiceIdAdder(session("invoiceId").as[String])).get))
      ).asJson
      .check(status is 201)
      .check(jsonPath("$.documentMetadata.id").saveAs("documentId"))
    )

  // ----------------------------------- GET Invoice, Invoice-Line, Document -----------------------------------

  val get_invoice = exec(http("GET Invoice")
    .get("/invoice/invoices/${invoiceId}")
    .header("x-okapi-token", x_okapi_token)
    .check(status is 200)
  )

  val get_invoice_line = exec(http("GET Invoice Line")
    .get("/invoice/invoice-lines/${invoiceLineId}")
    .header("x-okapi-token", x_okapi_token)
    .check(status is 200)
  )

  val get_invoice_document = exec(http("GET Invoice Document")
    .get("/invoice/invoices/${invoiceId}/documents/${documentId}")
    .header("x-okapi-token", x_okapi_token)
    .check(status is 200)
  )

  // ----------------------------------- PUT Invoice, Invoice-Line -----------------------------------

  val put_invoice = exec(http("PUT Invoice")
    .put("/invoice/invoices/${invoiceId}")
    .header("x-okapi-token", x_okapi_token)
    .body(StringBody(_ =>
      Json.stringify(invoice_body.as[JsObject] ++ Json.obj("status" -> "Approved")))
    ).asJson
    .check(status is 204)
  )

  val put_invoice_line = exec(http("PUT Invoice Line")
    .put("/invoice/invoice-lines/${invoiceLineId}")
    .header("x-okapi-token", x_okapi_token)
    .body(StringBody(session =>
      Json.stringify(invoice_line_body.as[JsObject] ++ Json.obj("subscriptionInfo" -> "-----------") ++ Json.obj("invoiceId" -> (session("invoiceId").as[String]))))
    ).asJson
    .check(status is 204)
  )

  // ----------------------------------- GET Invoices, Invoice-Lines, Documents Collection -----------------------------------

  val get_invoices_collection = exec(http("GET All Invoice")
    .get("/invoice/invoices")
    .header("x-okapi-token", x_okapi_token)
    .check(status is 200)
  )

  val get_invoice_lines_collection = exec(http("GET All Invoice Lines")
    .get("/invoice/invoice-lines")
    .header("x-okapi-token", x_okapi_token)
    .check(status is 200)
  )

  val get_invoice_documents_collection = exec(http("GET All Invoice Documents")
    .get("/invoice/invoices/${invoiceId}/documents")
    .header("x-okapi-token", x_okapi_token)
    .check(status is 200)
  )

  // ----------------------------------- DELETE Invoice, Invoice-Line -----------------------------------

  val delete_invoice_document = exec(http("DELETE Invoice Document")
    .delete("/invoice/invoices/${invoiceId}/documents/${documentId}")
    .header("x-okapi-token", x_okapi_token)
    .check(status is 204)
  )

  val delete_invoice_line = exec(http("DELETE Invoice Line")
    .delete("/invoice/invoice-lines/${invoiceLineId}")
    .header("x-okapi-token", x_okapi_token)
    .check(status is 204)
  )

  val delete_invoice = exec(http("DELETE Invoice")
    .delete("/invoice/invoices/${invoiceId}")
    .header("x-okapi-token", x_okapi_token)
    .check(status is 204)
  )

}
