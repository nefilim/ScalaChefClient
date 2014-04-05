package org.nefilim.chefclient.domain

import com.typesafe.scalalogging.slf4j.Logging
import org.scalatest.{FunSpec, Matchers}
import org.nefilim.chefclient.domain.ChefConstructs.{NodeIndexResultRow, ChefSearchResult}
import org.json4s._
import org.json4s.jackson.JsonMethods._

/**
 * Created by peter on 4/4/14.
 */
class EndpointsProtocolSpec extends FunSpec with Matchers with Logging {

  implicit val formats = DefaultFormats

  describe("deserialize sample search node response") {
    it("should deserialize successfully") {

      val json = """{
                   |  "start": 0,
                   |  "total": 1,
                   |  "rows": [
                   |    {
                   |      "name": "client-id",
                   |      "chef_environment": "localtest",
                   |      "json_class": "Chef::Node",
                   |      "automatic": {
                   |        "os": "linux",
                   |        "os_version": "3.4.73-64.112.amzn1.x86_64",
                   |        "hostname": "ip-10-0-30-193",
                   |        "fqdn": "ip-10-0-30-193.us-west-2.compute.internal",
                   |        "domain": "us-west-2.compute.internal",
                   |        "ipaddress": "10.0.30.193",
                   |        "uptime_seconds": 4941127,
                   |        "roles": [
                   |          "reporting-app",
                   |          "service",
                   |          "java7"
                   |        ]
                   |      },
                   |      "normal": {
                   |        "tags": [
                   |
                   |        ]
                   |      },
                   |      "chef_type": "node",
                   |      "default": {
                   |      },
                   |      "run_list": [
                   |        "role[reporting-app]"
                   |      ]
                   |    }
                   |  ]
                   |}""".stripMargin

      val jsonAST = parse(json)
      val searchResult = jsonAST.extract[ChefSearchResult[NodeIndexResultRow]]
      searchResult.rows.size should be (1)
      searchResult.rows(0).name should be ("client-id")
      searchResult.rows(0).chef_environment should be ("localtest")
      searchResult.rows(0).chef_type should be ("node")
      searchResult.rows(0).run_list.size should be (1)
      searchResult.rows(0).run_list(0) should be ("role[reporting-app]")
    }
  }

}
