package org.nefilim.chefclient

import com.typesafe.scalalogging.slf4j.Logging
import org.apache.commons.codec.binary.Base64
import spray.http._
import scala.concurrent.Future
import spray.client.pipelining._
import akka.actor.ActorSystem
import java.text.{SimpleDateFormat, DateFormat}
import java.util.TimeZone
import spray.http.HttpRequest
import spray.http.HttpHeaders.RawHeader
import scala.util.{Try, Failure, Success}
import spray.http.HttpResponse
import org.nefilim.chefclient.domain.Endpoints.{SearchResult, NodeIndexResultNode, ChefNode}
import spray.httpx.PipelineException
import org.nefilim.chefclient.domain.NodeIndex
import spray.httpx.encoding.Gzip
import spray.http.Uri.Query
import org.json4s._
import org.json4s.jackson.JsonMethods._

/**
 * Created by peter on 4/3/14.
 */

object ChefClient {
  implicit val system = ActorSystem("ChefClientActorSystem")
  implicit val formats = DefaultFormats

  val clientVersion = "11.4.0"
  val sign = "algorithm=sha1;version=1.0;"

  java.security.Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider())

  def apply(keyPath: String, clientId: String, host: String = "api.opscode.com", organizationPath: Option[String] = None) = {
    new ChefClient(
      keyPath,
      clientId,
      host.stripSuffix("/"),
      if (organizationPath.isDefined && !organizationPath.get.startsWith("/")) Some("/" + organizationPath.get) else organizationPath
    )
  }
}

import ChefClient._
class ChefClient(keyPath: String, clientId: String, host: String, organizationPath: Option[String]) extends Logging {
  private val privateKey = ChefClientCryptUtil.getPrivateKey(keyPath)

  import system.dispatcher

  def logTheRequest(request: HttpRequest) {
    logger.debug("the HTTP request", request)
  }

  def logServerResponse(response: HttpResponse) {
    logger.debug("the HTTP response {}", response)
  }

  def addOptionalHeader(headersForRequest: HttpRequest => List[HttpHeader]): RequestTransformer = { request =>
    request.mapHeaders(headers => headersForRequest(request) ++ headers)
  }

  def authHeadersForRequest(request: HttpRequest): List[HttpHeader] = {
    val formatter: DateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
    formatter.setTimeZone(TimeZone.getTimeZone("UTC"))
    val timestamp = formatter.format(new java.util.Date())
    val method = HttpMethods.GET

    val hashedPath = Base64.encodeBase64String(ChefClientCryptUtil.sha1(request.uri.path.toString()))
    val hashedBody = Base64.encodeBase64String(ChefClientCryptUtil.sha1(request.entity.asString))

    val canonicalRequest = s"Method:$method\nHashed Path:$hashedPath\nX-Ops-Content-Hash:$hashedBody\nX-Ops-Timestamp:$timestamp\nX-Ops-UserId:$clientId"
    val authorizationHeader: String = Base64.encodeBase64String(ChefClientCryptUtil.signData(canonicalRequest.getBytes, privateKey))

    var i = 0
    val authHeaders = authorizationHeader.sliding(60, 60).toList.map { line =>
      i += 1
      RawHeader("X-Ops-Authorization-" + i, line)
    } ++ List(RawHeader("X-Ops-Timestamp", timestamp), RawHeader("X-Ops-Content-Hash", hashedBody))
    authHeaders
  }

  val pipeline: HttpRequest => Future[HttpResponse] = (
      addOptionalHeader(authHeadersForRequest)
      ~> addHeader("X-Chef-Version", ChefClient.clientVersion)
      ~> addHeader("X-Ops-UserId", clientId)
      ~> addHeader("X-Ops-Sign", ChefClient.sign)
      ~> addHeader("Accept", ContentTypes.`application/json`.mediaType.value)
      ~> addHeader("Accept-Encoding", HttpEncodings.gzip.value)
      ~> logRequest(request => logTheRequest(request))
      ~> sendReceive
      ~> decode(Gzip)
      ~> logResponse(response => logServerResponse(response))
    )

  def nodeList(): Future[List[ChefNode]] = {
    val futureResponse = fireRequest("/nodes")
    futureResponse.map[List[ChefNode]] { response =>
      Try(parse(response.entity.asString).extract[Map[String, String]]) match {
        case Failure(failure) =>
          logger.error(s"failed to deserialize response ${response.entity.asString}", failure)
          throw new PipelineException(failure.toString)
        case Success(nodeList) =>
          nodeList.toList.map { case (k, v) => ChefNode(k, v) }
      }
    }
  }

  def searchNodeIndex(query: String, start: Int = 0, rows: Int = 1000, sort: String = ""): Future[SearchResult[NodeIndexResultNode]] = {
    val futureResponse = fireRequest("/search" + NodeIndex.path, Query("q" -> query, "rows" -> rows.toString, "start" -> start.toString, "sort" -> sort))
    futureResponse.map { response =>
      parse(response.entity.asString).extract[SearchResult[NodeIndexResultNode]]
    }
  }

  private[chefclient] def uri(scheme: String = "https", path: String, port: Int = 443, query: Query) = {
    Uri.from(
      scheme = scheme,
      host = host,
      port = port,
      path = path,
      query = query
    )
  }

  private[chefclient] def fireRequest(requestPath: String, query: Query = Query.Empty, body: Option[String] = None): Future[HttpResponse] = {
    // TODO build appropriate Method
    pipeline(Get(uri(path = organizationPath.getOrElse("") + requestPath, query = query)))
  }
}
