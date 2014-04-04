package org.nefilim.chefclient.domain


/**
 * Created by peter on 4/4/14.
 */
object Endpoints {
  case class ChefNode(name: String, uri: String)
  case class NodeList(nodes: List[ChefNode])

  case class SearchResult[T](total: Int, start: Int, rows: List[T])
  case class NodeIndexResultNode(name: String, chef_environment: String, json_class: String, chef_type: String, run_list: List[String])

  case class ResourceCreationResponse(uri: String)
}

sealed trait ChefSearchIndex {
  val path: String
}
case object ClientIndex extends ChefSearchIndex { val path = "/client"}
case object EnvironmentIndex extends ChefSearchIndex { val path = "/environment"}
case object NodeIndex extends ChefSearchIndex { val path = "/node"}
case object RoleIndex extends ChefSearchIndex { val path = "/role"}
