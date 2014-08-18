package edu.nyu.dlts.drpbx

import org.scalatra.{Accepted, FutureSupport, ScalatraServlet}
import org.scalatra.json._
import org.json4s.{ DefaultFormats, Formats }
import _root_.akka.actor.{ Actor, ActorRef, ActorSystem, Props }
import _root_.akka.pattern.ask
import _root_.akka.util.Timeout
import edu.nyu.dlts.drpbx.backend.domain._
import edu.nyu.dlts.drpbx.backend.domain.DBProtocol._
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._


class Backend(system: ActorSystem ) extends DrpbxBackendStack with JacksonJsonSupport with FutureSupport {
  protected implicit val jsonFormats: Formats = DefaultFormats
  implicit val timeout = new Timeout(2 seconds)
  protected implicit def executor: ExecutionContext = system.dispatcher
  
  val dbActor = system.actorOf(Props[DbActor])

  before() {
    contentType = formats("json")
  }

  get("/") {
    "nothing here"
  }

  get("/admin") {
    Map("admin" -> true)
  }

  get("/admin/create") {
    dbActor ! Create
    Map("admin" -> Map("create" -> true))
  }

  get("/admin/drop") {
    dbActor ! Drop
    Map("admin" -> Map("drop" -> true))
  }

  get("/admin/purge") {
    dbActor ! Purge
    Map("admin" -> Map("purge" -> true))
  }
}

class DbActor extends Actor with DrpbxDbSupport {
  def receive = {
  	
    case Create => {
      println("CREATE MESSAGE RECEIVED")
      m.createDB
      println("DB CREATED")
    }

    case Drop => {
      println("DROP MESSAGE RECEIVED")
      m.dropDB
      println("DB DROPPED")
    }

    case Purge => {
      println("PURGE MESSAGE RECIEVED")
      m.dropDB
      m.createDB
      println("DB PURGED")
    }
  }
}