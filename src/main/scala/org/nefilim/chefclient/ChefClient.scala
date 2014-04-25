package org.nefilim.chefclient

import com.typesafe.scalalogging.slf4j.Logging
import org.apache.commons.codec.binary.Base64
import spray.http._
import scala.concurrent.Future
import spray.client.pipelining._
import akka.actor.ActorSystem
import java.text.{SimpleDateFormat, DateFormat}
import java.util.TimeZone
import scala.util.Try
import org.nefilim.chefclient.domain.ChefConstructs._
import org.nefilim.chefclient.domain.NodeIndex
import spray.httpx.encoding.Gzip
import spray.http.Uri.Query
import org.json4s._
import org.json4s.jackson.JsonMethods._
import spray.http.HttpRequest
import org.nefilim.chefclient.domain.ChefConstructs.ChefNode
import org.nefilim.chefclient.domain.ChefConstructs.NodeIndexResultRow
import spray.http.HttpHeaders.RawHeader
import org.nefilim.chefclient.domain.ChefConstructs.ChefSearchResult
import scala.util.Failure
import scala.Some
import spray.http.HttpResponse
import scala.util.Success

/**
 * Created by peter on 4/3/14.
 */

object ChefClient {
  implicit val system = ActorSystem("ChefClientActorSystem")
  implicit val formats = DefaultFormats

  val clientVersion = "11.4.0"
  val sign = "algorithm=sha1;version=1.0;"

  java.security.Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider())

  def apply(keyPath: String, clientId: String, host: String = "api.opscode.com", organizationPath: Option[String] = None): ChefClient = {
    new ConfiguredChefClient(
      keyPath,
      clientId,
      host.stripSuffix("/"),
      if (organizationPath.isDefined && !organizationPath.get.startsWith("/")) Some("/" + organizationPath.get) else organizationPath
    )
  }
}

import ChefClient._
trait ChefClient {
  def nodeList(): Future[Either[ChefClientFailedResult, List[ChefNode]]]
  def searchNodeIndex(query: String, start: Int = 0, rows: Int = 1000, sort: String = ""): Future[Either[ChefClientFailedResult, ChefSearchResult[NodeIndexResultRow]]]
  def deleteNode(node: String): Future[Either[ChefClientFailedResult, LastKnownNodeState]]
  def deleteClient(client: String): Future[Either[ChefClientFailedResult, HttpResponse]]
}

class ConfiguredChefClient(keyPath: String, clientId: String, host: String, organizationPath: Option[String]) extends ChefClient with Logging {
  private val privateKey = ChefClientCryptUtil.getPrivateKey(keyPath)

  import system.dispatcher

  private[chefclient] def logTheRequest(request: HttpRequest) {
    logger.debug("the HTTP request {}", request)
  }

  private[chefclient] def logServerResponse(response: HttpResponse) {
    logger.debug("the HTTP response {}", response)
  }

  private[chefclient] def addOptionalHeader(headersForRequest: HttpRequest => List[HttpHeader]): RequestTransformer = { request =>
    request.mapHeaders(headers => headersForRequest(request) ++ headers)
  }

  private[chefclient] def authHeadersForRequest(request: HttpRequest): List[HttpHeader] = {
    val formatter: DateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
    formatter.setTimeZone(TimeZone.getTimeZone("UTC"))
    val timestamp = formatter.format(new java.util.Date())
    val method = request.method

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

  private[chefclient] lazy val pipeline: HttpRequest => Future[HttpResponse] = (
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

  private def extractResponse[T : Manifest](httpResponse: HttpResponse): Either[ChefClientFailedResult, T] = {
    httpResponse match {
      case r if r.status.isSuccess =>
        Try(parse(httpResponse.entity.asString).extract[T]) match {
          case Failure(failure) =>
            logger.error(s"failed to deserialize response ${httpResponse.entity.asString}", failure)
            Left(ChefClientFailedResult(httpResponse, Some(failure)))
          case Success(result) =>
            Right(result)
        }
      case _ =>
        logger.error("the request failed {}", httpResponse)
        Left(ChefClientFailedResult(httpResponse, None))
    }
  }

  def nodeList(): Future[Either[ChefClientFailedResult, List[ChefNode]]] = {
    val futureResponse = fireRequest("/nodes")
    futureResponse.map(httpResponse => extractResponse[Map[String, String]](httpResponse)).map {
      case Right(nodeList) =>
        Right(nodeList.toList.map { case (k, v) => ChefNode(k, v) })
      case Left(l) =>
        Left(l)
    }
  }

  def searchNodeIndex(query: String, start: Int = 0, rows: Int = 1000, sort: String = ""): Future[Either[ChefClientFailedResult, ChefSearchResult[NodeIndexResultRow]]] = {
    val futureResponse = fireRequest("/search" + NodeIndex.path, Query("q" -> query, "rows" -> rows.toString, "start" -> start.toString, "sort" -> sort))
    futureResponse.map(extractResponse[ChefSearchResult[NodeIndexResultRow]])
  }

  def deleteNode(node: String): Future[Either[ChefClientFailedResult, LastKnownNodeState]] = {
    fireRequest("/nodes/" + node, method = HttpMethods.DELETE).map(extractResponse[LastKnownNodeState](_))
  }

  def deleteClient(client: String): Future[Either[ChefClientFailedResult, HttpResponse]] = {
    fireRequest("/clients/" + client, method = HttpMethods.DELETE).map { response =>
      response match {
        case r if r.status.isSuccess => Right(response)
        case _ => Left(ChefClientFailedResult(response))
      }
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

  private[chefclient] def fireRequest(requestPath: String, query: Query = Query.Empty, body: Option[String] = None, method: HttpMethod = HttpMethods.GET): Future[HttpResponse] = {
    // TODO build appropriate Method
    method match {
      case HttpMethods.GET =>
        pipeline(Get(uri(path = organizationPath.getOrElse("") + requestPath, query = query)))
      case HttpMethods.DELETE =>
        pipeline(Delete(uri(path = organizationPath.getOrElse("") + requestPath, query = query)))
    }
  }
}

