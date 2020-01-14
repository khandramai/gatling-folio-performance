package org.folio.mod_invoices

import java.io.File

import com.typesafe.config.ConfigFactory
import io.gatling.core.Predef._
import org.folio.mod_invoices.InvoicesCrudHelper._

/**
  * This test simulate the open model performance load, i.e. the following scenario:
  * given number of users with a linear ramp over a given duration make call Orders CRUD API.
  *
  * Mostly this scenario can be used to determine the number of VU/requests per second at which a service failure occurs.
  */
class OpenModelSimulation extends Simulation {

  val config = ConfigFactory.parseFile(new File(getClass.getResource("/test.conf").toURI))
  val users = config.getInt("users")
  val period = config.getInt("period")

  val crud_scn = scenario("Open model Orders CRUD simulation").exec(post_invoice).exec(approve_invoice).exec(get_invoice).exec(delete_invoice)

  setUp(crud_scn.inject(constantUsersPerSec(5) during period)).protocols(https)

}
