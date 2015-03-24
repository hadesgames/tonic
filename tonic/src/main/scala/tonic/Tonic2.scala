package tonic

import scala.concurrent.duration._
import akka.io.IO
import akka.actor.ActorSystem
import spray.routing.RequestContext
import spray.routing.Route
import spray.http.HttpRequest
import spray.http.Uri
import spray.can.Http
import spray.routing.SimpleRoutingApp


trait ProxyDirectives {

  private def sending(f: RequestContext => HttpRequest)(implicit system: ActorSystem): RequestContext => Unit = {
    val transport = IO(Http)
    ctx => {
      // Send the request to the Http transport and send back the response to ctx.responder
      transport.tell(f(ctx), ctx.responder)
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

  startServer(interface="localhost", port = 8080) {
    path("quit") {
      complete {
        system.shutdown()
        "Good bye"
      }
    } ~
    proxyTo("http://localhost:8000")
  }
}

