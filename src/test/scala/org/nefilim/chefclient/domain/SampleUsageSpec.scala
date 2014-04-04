package org.nefilim.chefclient.domain

import org.scalatest.{DoNotDiscover, FunSpec, Matchers}
import com.typesafe.scalalogging.slf4j.Logging
import scala.util.{Failure, Success}
import org.nefilim.chefclient.ChefClient
import spray.testkit.ScalatestRouteTest

/**
 * Created by peter on 4/4/14.
 */
@DoNotDiscover
class SampleUsageSpec extends FunSpec with Matchers with ScalatestRouteTest with Logging {

  def actorRefFactory = system

  describe("sample usage") {
    it("should work") {
      val keyPath = "theuser.pem"
      val chefClient = ChefClient(keyPath, "theuser", "api.opscode.com", Some("/organizations/myorg"))
      chefClient.nodeList().onComplete {
        case Success(success) =>
          success.foreach(logger.info("node {}", _))
        case Failure(failure) =>
          logger.error("node list failure {}", failure)
      }

      chefClient.searchNodeIndex("name:*").onComplete {
        case Success(searchResult) =>
          searchResult.rows.foreach(logger.info("node {}", _))
        case Failure(failure) =>
          logger.error("search result failure {}", failure)
      }
    }
  }
}
