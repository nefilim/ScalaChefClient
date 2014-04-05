ChefClientScala
===============

Very minimal Chef Server async client written in Scala (2.10) with Spray (1.3.1).

Only supports node listing and node index search right now, with limited response parsing of the search results (see [SearchResults]).


Sample Usage
------------

```scala
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
```


[SearchResults]:https://github.com/nefilim/ChefClientScala/blob/master/src/main/scala/org/nefilim/chefclient/domain/ChefConstructs.scala
