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



object Client extends App with  SimpleRoutingApp{
  implicit val system = ActorSystem()
  startServer(interface="localhost", port = 8000) {
    path("quit") {
      complete {
        system.shutdown()
        "Ok!"
      }
    } ~
    complete {
      "Ok!"
    }
  }
}
