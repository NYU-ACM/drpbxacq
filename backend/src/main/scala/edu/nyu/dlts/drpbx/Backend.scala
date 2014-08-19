package edu.nyu.dlts.drpbx

import _root_.akka.actor.{ Actor, ActorRef, ActorSystem, Props }
import _root_.akka.pattern.ask
import _root_.akka.util.Timeout
import org.json4s.{ DefaultFormats, Formats }
import org.scalatra.{Accepted, FutureSupport, ScalatraServlet}
import org.scalatra.json._
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import scala.concurrent.{ ExecutionContext, Future, Await }
import scala.concurrent.duration._
import edu.nyu.dlts.drpbx.backend.domain.DrpbxDbSupport
import edu.nyu.dlts.drpbx.backend.domain.DBProtocol._

import org.apache.commons.codec.binary.Hex
import java.security.MessageDigest  
import java.util.UUID

class Backend(system: ActorSystem ) extends DrpbxBackendStack with JacksonJsonSupport with FutureSupport {
  
  protected implicit val jsonFormats: Formats = DefaultFormats
  implicit val timeout = new Timeout(2 seconds)
  protected implicit def executor: ExecutionContext = system.dispatcher
  
  val dbActor = system.actorOf(Props[DbActor], name = "db")

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

  get("/admin/insert") {
    val uuid = UUID.randomUUID
    val md5 = MessageDigest.getInstance("MD5").digest(params("password").getBytes)
    val md5Hex = new String(Hex.encodeHexString(md5))
    val admin = new Admin(uuid, params("name"), md5Hex)
    dbActor ! admin
    admin
  }

  get("/admin/login") {
    implicit val timeout = Timeout(5 seconds)
    val md5 = MessageDigest.getInstance("MD5").digest(params("password").getBytes)
    val md5Hex = new String(Hex.encodeHexString(md5))
    val login = new Login(params("name"), md5Hex)
    val future = dbActor ? login    
    val result = Await.result(future, timeout.duration).asInstanceOf[Option[Admin]]
    
    result match {
      case Some(a) => Map("result" -> true, "name" -> a.name)
      case None => Map("result" -> false)
    }
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

    case admin: Admin => {
      println("INSERT MESSAGE RECEIVED")
      m.insertAdmin(admin)
      println("ADMIN INSERTED")
    }

    case login: Login => {
      println("LOGIN MESSAGE RECEIVED")
      sender ! m.loginAdmin(login)
    }
  }
}