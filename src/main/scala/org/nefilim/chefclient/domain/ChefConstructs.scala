package org.nefilim.chefclient.domain


/**
 * Created by peter on 4/4/14.
 */
object ChefConstructs {
  case class ChefNode(name: String, uri: String)
  case class NodeList(nodes: List[ChefNode])

  case class ChefSearchResult[T <: ChefSearchResultRow](total: Int, start: Int, rows: List[T])
  sealed trait ChefSearchResultRow
  case class NodeIndexResultRow(
                 name: String,
                 chef_environment: String,
                 automatic: OhaiValues,
                 json_class: String,
                 chef_type: String,
                 run_list: List[String]) extends ChefSearchResultRow

  case class OhaiValues(
                 os: String,
                 os_version: String,
                 hostname: String,
                 fqdn: String,
                 domain: String,
                 ipaddress: String,
                 uptime_seconds: Int,
                 roles: List[String],
                 ec2: Option[EC2Values])

  case class EC2Values(
                ami_id: String,
                block_device_mapping_ami: String,
                block_device_mapping_root: String,
                hostname: String,
                instance_action: String,
                instance_id: String,
                kernel_id: String,
                mac: String)

  case class ResourceCreationResponse(uri: String)
}

sealed trait ChefSearchIndex {
  val path: String
}
case object ClientIndex extends ChefSearchIndex { val path = "/client"}
case object EnvironmentIndex extends ChefSearchIndex { val path = "/environment"}
case object NodeIndex extends ChefSearchIndex { val path = "/node"}
case object RoleIndex extends ChefSearchIndex { val path = "/role"}
