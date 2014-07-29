package drpbxacq

import akka.actor._
import akka.util.Timeout

import scala.concurrent.duration._
import spray.routing._
import spray.routing.RequestContext
import spray.http.StatusCodes
import spray.httpx.SprayJsonSupport._


class DrpbxacqBackend extends HttpServiceActor with RestApi {
  def receive = runRoute(routes) 
}

trait RestApi extends HttpService with ActorLogging { actor: Actor =>
  import context.dispatcher
  import akka.pattern.{ask, pipe}

  implicit val timeout = Timeout(10 seconds)
  
  val routes: Route = {
    path("") {
      get { 
        complete {
          "alive"
        }
      }
    }
  }
}
