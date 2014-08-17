package edu.nyu.dlts.drpbx

import org.scalatra.{Accepted, FutureSupport, ScalatraServlet}
import org.scalatra.json._
import org.json4s.{ DefaultFormats, Formats }
import _root_.akka.actor.{ Actor, ActorRef, ActorSystem, Props }
import _root_.akka.pattern.ask
import _root_.akka.util.Timeout
import edu.nyu.dlts.drpbx.backend.domain._
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._


class Backend(system: ActorSystem ) extends DrpbxBackendStack with JacksonJsonSupport with FutureSupport {
  protected implicit val jsonFormats: Formats = DefaultFormats
  implicit val timeout = new Timeout(2 seconds)
  protected implicit def executor: ExecutionContext = system.dispatcher

  before() {
    contentType = formats("json")
  }

  get("/") {
  	val echo = system.actorOf(Props[EchoActor])
    echo ! "test"
    Accepted("accept")
  }
}

class EchoActor extends Actor {
	def receive = {
	  case msg => {
	  	for(i <- 1 to 100000000){println(i + " " + msg)}
	  }
	}
}