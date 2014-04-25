ChefClientScala
===============

Very minimal Chef Server async client written in Scala (2.10) with Spray (1.3.1). The motivation for this library is to do network discovery and drive an operational dashboard.

Supported operations
* List all nodes
* Search the node index
* Deleting a node
* Deleting a client


Sample Usage
------------

Operations returns:
```scala
Future[Either[ChefClientFailedResult, T]]
```

Usage:
```scala
      val chefClient = ChefClient(keyPath, "theuser", "api.opscode.com", Some("/organizations/myorg"))
      val f1 = chefClient.nodeList()
      f1.onComplete {
        case Success(success) =>
          success match {
            case Right(r) =>
              r.foreach(logger.info("node {}", _))
            case Left(l) =>
              logger.error("failed {}", l)
          }
        case Failure(failure) =>
          logger.error("node list failure {}", failure)
      }

      val f2 = chefClient.searchNodeIndex("name:*")
      f2.onComplete {
        case Success(searchResult) =>
          searchResult match {
            case Right(r) =>
              r.rows.foreach(logger.info("node {}", _))
            case Left(l) =>
              logger.error("failed {}", l)
          }
        case Failure(failure) =>
          logger.error("search result failure {}", failure)
      }

      val f3 = chefClient.deleteClient("app-i-e3ade6eb")
      f3.onComplete {
        case Success(result) =>
          logger.info("deleted client {}", result)
          result.isRight should be (true)
          result.right.get.status should be (StatusCodes.OK)
        case Failure(failure) =>
          logger.error("delete client failure {}", failure)
      }

      val f4 = chefClient.deleteNode("app-i-e3ade6eb")
      f4.onComplete {
        case Success(result) =>
          logger.info("deleted node {}", result)
          result.isRight should be (true)
        case Failure(failure) =>
          logger.error("delete node failure {}", failure)
      }
```


[SearchResults]:https://github.com/nefilim/ChefClientScala/blob/master/src/main/scala/org/nefilim/chefclient/domain/ChefConstructs.scala
