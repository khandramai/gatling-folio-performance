package org.folio.mod_orders

import java.io.File

import com.typesafe.config.ConfigFactory
import io.gatling.core.Predef._
import org.folio.mod_orders.OrdersCrudHelper._

/**
  * This test simulate the following scenario: one/several VU sequentially calls Orders CRUD API predetermined number of times.
  * Such load regime allows to measure performance actual throughput requests/sec for each CRUD endpoint for sequential
  * requests repeated in one/several threads. Number of VU can be increased to investigate dependency of performance from number of threads.
  *
  * This scenario corresponds JMeter approach when requests are executed cyclically in each of the thread groups (here VUs
  * play role of thread group).
  *
  */
class MaxThroughputIterativeSimulation extends Simulation {

  val config = ConfigFactory.parseFile(new File(getClass.getResource("/test.conf").toURI))
  val iterations = config.getInt("iterations")

  val crud_scn = scenario("Iterative Orders CRUD simulation")
    .repeat(iterations) {
    exec(post).exec(put).exec(get).exec(delete)
  }

  setUp(crud_scn.inject(atOnceUsers(1))).protocols(https)

}
