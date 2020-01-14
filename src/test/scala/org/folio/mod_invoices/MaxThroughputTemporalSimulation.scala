package org.folio.mod_invoices

import java.io.File

import com.typesafe.config.ConfigFactory
import io.gatling.core.Predef._
import org.folio.mod_invoices.InvoicesCrudHelper._

/**
  * This test simulate the following scenario: one/several VU sequentially calls Orders CRUD API during fixed time.
  * Such load regime allows to measure performance actual throughput requests/sec for each CRUD endpoint for sequential
  * requests repeated in one/several threads. Number of VU can be increased to investigate dependency of performance from number of threads.
  *
  * This scenario corresponds JMeter approach when requests are executed cyclically in each of the thread groups (here VUs
  * play role of thread group).
  *
  */
class MaxThroughputTemporalSimulation extends Simulation {

  val config = ConfigFactory.parseFile(new File(getClass.getResource("/test.conf").toURI))
  val duration = config.getInt("duration")

  val crud_scn = scenario("Temporal Invoices CRUD simulation")
    .during(duration) {
    exec(post_invoice).exec(post_invoice_line).exec(post_invoice_document)
      .exec(get_invoice).exec(get_invoice_line).exec(get_invoice_document)
      .exec(approve_invoice).exec(pay_invoice)
      .exec(get_invoices_collection).exec(get_invoice_lines_collection).exec(get_invoice_documents_collection)
      .exec(delete_invoice_line).exec(delete_invoice_document)
  }

  setUp(crud_scn.inject(atOnceUsers(25))).protocols(https)

}
