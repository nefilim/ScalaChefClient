package org.nefilim.chefclient.domain

import org.scalatest.{FunSpec, Matchers}
import com.typesafe.scalalogging.slf4j.Logging
import scala.util.{Failure, Success}
import org.nefilim.chefclient.ChefClient
import spray.testkit.ScalatestRouteTest
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps
import spray.http.StatusCodes

/**
 * Created by peter on 4/4/14.
 */
//@DoNotDiscover
class SampleUsageSpec extends FunSpec with Matchers with ScalatestRouteTest with Logging {

  def actorRefFactory = system

  describe("sample usage") {
    it("should work") {
      val keyPath = "/Users/peter/develop/mino/infrastructure/chef-repo/.chef/peterv.pem"
      val chefClient = ChefClient(keyPath, "peterv", "api.opscode.com", Some("/organizations/minomonsters"))
//      val keyPath = "/Users/peter/develop/mino/snsMonitor/client.pem"
//      val chefClient = ChefClient(keyPath, "SNSMonitor", "api.opscode.com", Some("/organizations/minomonsters"))
//      val f1 = chefClient.nodeList()
//      f1.onComplete {
//        case Success(success) =>
//          success match {
//            case Right(r) =>
//              r.foreach(logger.info("node {}", _))
//            case Left(l) =>
//              logger.error("failed {}", l)
//          }
//        case Failure(failure) =>
//          logger.error("node list failure {}", failure)
//      }
//
//      val f2 = chefClient.searchNodeIndex("namep:*")
//      f2.onComplete {
//        case Success(searchResult) =>
//          searchResult match {
//            case Right(r) =>
//              r.rows.foreach(logger.info("node {}", _))
//            case Left(l) =>
//              logger.error("failed {}", l)
//          }
//        case Failure(failure) =>
//          logger.error("search result failure {}", failure)
//      }

      val f3 = chefClient.deleteClient("ToastyPipeRC3-collector-app-i-e3ade6eb")
      f3.onComplete {
        case Success(result) =>
          logger.info("deleted client {}", result)
          result.isRight should be (true)
          result.right.get.status should be (StatusCodes.OK)
        case Failure(failure) =>
          logger.error("delete client failure {}", failure)
      }

      val f4 = chefClient.deleteNode("ToastyPipeRC3-collector-app-i-e3ade6eb")
      f4.onComplete {
        case Success(result) =>
          logger.info("deleted node {}", result)
          result.isRight should be (true)
        case Failure(failure) =>
          logger.error("delete node failure {}", failure)
      }

//      Await.ready(f1, (30 seconds))
//      Await.ready(f2, (30 seconds))
      Await.ready(f3, (30 seconds))
      Await.ready(f4, (30 seconds))
    }
  }
}
