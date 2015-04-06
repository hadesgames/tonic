package tonic

import scala.concurrent.duration._
import akka.io.IO
import akka.actor.ActorSystem
import akka.pattern.{ask, pipe}
import spray.routing.RequestContext
import spray.routing.Route
import spray.http.HttpRequest
import spray.http.HttpResponse
import spray.http.Uri
import spray.can.Http
import spray.routing.SimpleRoutingApp
import scala.concurrent.Future


trait ProxyDirectives {

  private def sending(f: RequestContext => HttpRequest)(implicit system: ActorSystem): RequestContext => Unit = {
    import system.dispatcher
    val transport = IO(Http)
    ctx => {
      // Send the request to the Http transport and send back the response to ctx.responder
      val respFt: Future[HttpResponse] = transport.ask(f(ctx))(5 seconds).mapTo[HttpResponse]

      val modifiedResp = respFt.map { resp =>
        resp.copy(
          headers = resp.headers.filterNot({ header =>
            header.is("content-length") || header.is("date") || header.is("content-type") || header.is("server")
          })
        )
      }

      modifiedResp pipeTo ctx.responder
    }
  }


  def proxyTo(uri: Uri)(implicit system: ActorSystem): Route = {
    sending(ctx => {
      ctx.request.copy(
        uri = uri.withPath(uri.path ++ ctx.unmatchedPath),
        headers = ctx.request.headers.filterNot(_.is("host"))
      )
    })
  }
}


object Tonic extends App with  SimpleRoutingApp with ProxyDirectives {
  implicit val system = ActorSystem()

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

