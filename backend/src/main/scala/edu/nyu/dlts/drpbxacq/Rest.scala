package drpbxacq

import akka.actor._
import akka.pattern.{ ask, pipe }
import akka.util.Timeout

import scala.concurrent.duration._
import spray.routing._
import spray.http._

import Directives._
import MediaTypes._


class DrpbxacqBackend extends HttpServiceActor with RestApi {
  def receive = runRoute(routes)
}

trait RestApi extends HttpService with ActorLogging { actor: Actor =>
  import context.dispatcher
  implicit val timeout = Timeout(10 seconds)

  def routes: Route =
    path("transfers" / IntNumber) { id =>
      get {
        respondWithMediaType(`text/html`) {
          complete {
            <html><body>transfer: {id}</body></html>
          }
        }
      }
    }
}