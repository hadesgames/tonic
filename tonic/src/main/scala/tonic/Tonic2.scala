package tonic

import scala.concurrent.duration._
import akka.io.IO
import akka.actor.ActorSystem
import akka.pattern.{ask, pipe}
import spray.routing.RequestContext
import spray.routing.Route
import spray.http.HttpRequest
import spray.http.StatusCodes
import spray.http.HttpResponse
import spray.http.Uri
import spray.can.Http
import spray.routing.SimpleRoutingApp
import scala.concurrent.Future

import redis.RedisClient

import scala.util.{Try, Success, Failure}
import scala.concurrent.ExecutionContext


trait ProxyDirectives {
  private case object NoSession extends Exception

  val redis: RedisClient
  val prefix: String
  val cookie: String

  private def sending(f: RequestContext => Future[Try[HttpRequest]])(implicit system: ActorSystem): RequestContext => Unit = {
    import system.dispatcher
    val transport = IO(Http)
    ctx => {
      f(ctx) map {
        case Success(req) => {
          // Send the request to the Http transport and send back the response to ctx.responder
          val respFt: Future[HttpResponse] = transport.ask(req)(5 seconds).mapTo[HttpResponse]

          val modifiedResp = respFt.map { resp =>
            resp.copy(
              headers = resp.headers.filterNot({ header =>
                header.is("content-length") || header.is("date") || header.is("content-type") || header.is("server")
              })
            )
          }

          modifiedResp pipeTo ctx.responder
        }
        case Failure(_) => {
          //ctx.responder ! HttpResponse(StatusCodes.Unauthorized)
          ctx.responder ! HttpResponse(StatusCodes.OK)
        }
      }
    }
  }


  def checkRequest(req: HttpRequest)(implicit ec: ExecutionContext): Future[Try[HttpRequest]] = {
    val key = prefix + req.cookies.find({ c =>
      c.name == cookie
    }).map(_.content).getOrElse("<none>")

    redis.exists(key).map { res =>
      if (res)
        Success(req)
      else
        Failure(NoSession)
    }
  }

  def extractRequest(ctx: RequestContext, uri: Uri): HttpRequest = {
      ctx.request.copy(
        uri = uri.withPath(uri.path ++ ctx.unmatchedPath),
        headers = ctx.request.headers.filterNot(_.is("host"))
      )
  }


  def proxyTo(uri: Uri)(implicit system: ActorSystem): Route = {
    import system.dispatcher
    sending(ctx => { 
        val req = extractRequest(ctx, uri)
        checkRequest(req)
    })
  }
}


object Tonic extends App with  SimpleRoutingApp with ProxyDirectives {
  implicit val system = ActorSystem()
  lazy val redis = RedisClient()
  val prefix = "kuende:"
  val cookie = "SESSION"

  println(s"""Starting proxy on port ${args(0)} to ${System.getenv("UPSTREAM_URL")}""")

  startServer(interface="0.0.0.0", port = args(0).toInt) {
    path("quit") {
      complete {
        system.shutdown()
        "Good bye"
      }
    } ~
    proxyTo(System.getenv("UPSTREAM_URL"))
  }
}

