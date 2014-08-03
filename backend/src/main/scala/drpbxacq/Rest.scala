package drpbxacq

import akka.actor._
import akka.pattern.{ ask, pipe }
import akka.util.Timeout

import scala.concurrent.duration._
import spray.routing._
import spray.http._
import spray.httpx.Json4sSupport
import org.json4s.Formats
import org.json4s.DefaultFormats

import Directives._
import MediaTypes._

import drpbxacq.models._


class DrpbxacqBackend extends HttpServiceActor with RestApi {
  def receive = runRoute(routes)
}

trait RestApi extends HttpService with ActorLogging with Json4sSupport { actor: Actor =>
  import context.dispatcher
  implicit val timeout = Timeout(10 seconds)
  implicit def json4sFormats: Formats = DefaultFormats

  def routes: Route =
    path("transfers" / "[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}".r) { id =>
      get {
        respondWithMediaType(`application/json`) {
          complete {
            val message = new Message("success", id)
            message
          }
        }
      }
    }
}