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
  
  val logger: Logger = LoggerFactory.getLogger("drpbx.rest")
  protected implicit val jsonFormats: Formats = DefaultFormats
  implicit val timeout = new Timeout(2 seconds)
  protected implicit def executor: ExecutionContext = system.dispatcher
  
  val dbActor = system.actorOf(Props[DbActor], name = "db")
  val dnrActor = system.actorOf(Props[DonorActor], name = "donor")
  val fileActor = system.actorOf(Props[FileActor], name = "file")

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
    val admin = new Admin(uuid, params("email"), md5Hex)
    dbActor ! admin
    admin
  }

  get("/admin/login") {
    implicit val timeout = Timeout(5 seconds)
    val md5 = MessageDigest.getInstance("MD5").digest(params("password").getBytes)
    val md5Hex = new String(Hex.encodeHexString(md5))
    val login = new Login(params("email"), md5Hex)
    val future = dbActor ? login    
    val result = Await.result(future, timeout.duration).asInstanceOf[Option[Admin]]
    
    result match {
      case Some(a) => { Map("result" -> true, "name" -> a.name) }
      case None => Map("result" -> false)
    }
  }

  post("/donor/create") {
    val md5 = MessageDigest.getInstance("MD5").digest(params("password").getBytes)
    val md5Hex = new String(Hex.encodeHexString(md5))
    val donor = new Donor(UUID.randomUUID, params("email"), params("name"), params("org"), md5Hex, params("token"))
    dnrActor ! donor
    Map("result" -> true, "donor" -> donor.email)
  }

  get("/donor/login") {
    implicit val timeout = Timeout(5 seconds) 
    val md5 = MessageDigest.getInstance("MD5").digest(params("password").getBytes)
    val md5Hex = new String(Hex.encodeHexString(md5))
    val login = new Login(params("email"), md5Hex)
    val future = dnrActor ? login    
    val result = Await.result(future, timeout.duration).asInstanceOf[Option[Donor]]
    
    result match {
      case Some(donor) => { 
        logger.info("login validated: " + donor.email )
        Map("result" -> true, "id" -> donor.id.toString) 
      }
      case None => Map("result" -> false)
    }
  }

  get("/donor/:email/validate") {
    implicit val timeout = Timeout(5 seconds) 
    val email = new EmailReq(params("email"))
    val future = dnrActor ? email
    val result = Await.result(future, timeout.duration).asInstanceOf[Option[Donor]]

    result match {
      case Some(donor) => { 
        Map("result" -> true, "id" -> donor.id.toString)
      }
      case None => Map("result" -> false)
    }
  }

  get("/donor/:id/token") {
    implicit val timeout = Timeout(5 seconds)
    val tokenReq = new TokenReq(UUID.fromString(params("id")))
    val future = dnrActor ? tokenReq
    val result = Await.result(future, timeout.duration).asInstanceOf[Option[String]] 

    result match {
      case Some(token) => Map("result" -> true, "token" -> token) 
      case None => Map("result" -> false)
    }
  }

  post("/transfer") {
    implicit val timeout = Timeout(5 seconds)
    val xfer = parsedBody.extract[TransferReq]
    val future = dnrActor ? xfer
    val result = Await.result(future, timeout.duration).asInstanceOf[TransferResponse] 
    result.result match {
      case true => Map("result" -> true, "count" -> result.count)
      case false => Map("result" -> false)
    }
  }

  get("/transfers") {
    implicit val timeout = Timeout(10 seconds)  
    val future = dbActor ? TransferAll
    val result = Await.result(future, timeout.duration)
    Map("result" -> true, "transfers" -> result)
  }

  get("/transfer/:id") {
    implicit val timeout = Timeout(5 seconds)
    val future = dnrActor ? new TransferId(UUID.fromString(params("id")))
    val result = Await.result(future, timeout.duration)
    result match {
      case Some(transfer) => transfer
      case None => Map("result" -> false)
    }
  }

  get("/donor/:id/transfers") {
    implicit val timeout = Timeout(10 seconds)
    val transfer = new DonorTransfersReq(UUID.fromString(params("id")))
    val future = dnrActor ? transfer  
    val result = Await.result(future, timeout.duration)
    result match {
      case Some(xfers) => Map("result" -> true, "transfers" -> xfers) 
      case None => Map("result" -> false) 
    } 
  }

  get("/file/:id/show") {
    implicit val timeout = Timeout(5 seconds)
    val file = new FileReq(UUID.fromString(params("id")))
    val future = fileActor ? file
    Await.result(future, timeout.duration) match {
      case Some(file) => Map("result" -> true, "file" -> file) 
      case None => Map("result" -> false) 
    } 
  }

}

class FileActor extends Actor with DrpbxDbSupport {
  val logger: Logger = LoggerFactory.getLogger("drpbx.fileactor")
  def receive = {
    case req: FileReq => {
      sender ! Some("test")
    }
  }

}

class DbActor extends Actor with DrpbxDbSupport {
  val logger: Logger = LoggerFactory.getLogger("drpbx.rest")
  def receive = {
  	
    case Create => {
      logger.info("CREATE MESSAGE RECEIVED")
      m.createDB
      logger.info("DB CREATED")
    }

    case Drop => {
      logger.info("DROP MESSAGE RECEIVED")
      m.dropDB
      logger.info("DB DROPPED")
    }

    case Purge => {
      logger.info("PURGE MESSAGE RECIEVED")
      m.dropDB
      m.createDB
      logger.info("DB PURGED")
    }

    case admin: Admin => {
      logger.info("INSERT MESSAGE RECEIVED")
      m.insertAdmin(admin)
      logger.info("ADMIN INSERTED")
    }

    case login: Login => {
      logger.info("ADMIN LOGIN MESSAGE RECEIVED")
      sender ! m.loginAdmin(login)
    }

    case TransferAll => {
      logger.info("ALL TRANSFERS MESSAGE RECEIVED")
      sender ! m.getTransfers
    }
  }
}

class DonorActor extends Actor with DrpbxDbSupport {
  val logger: Logger = LoggerFactory.getLogger("drpbx.rest")
  def receive = {
    case donor: Donor => {
      logger.info("DONOR INSERT MSG RECEIVED")
      m.insertDonor(donor)
      logger.info("DONOR INSERTED")
    }

    case login: Login => {
      logger.info("LOGIN MSG RECEIVED")
      sender ! m.loginDonor(login)
    }

    case req: EmailReq => {
      logger.info("Email Request Received")
      sender ! m.getDonor(req.email)
    }

    case req: TokenReq => {
      logger.info("TOKEN REQUEST RECEIVED")
      sender ! m.getDonorToken(req.id)
    }

    case req: TransferReq => {
      logger.info("TRANSFER REQUEST RECEIVED")
      sender ! m.insertTransfer(req)
    }

    case req: DonorTransfersReq => {
      logger.info("DONOR TRANSFER REQUEST RECEIVED")
      sender ! m.getDonorTransfers(req)
    }

    case req: TransferId =>
      logger.info("TRANSFER REQUEST RECEIVED")
      val transfer = m.getTransferById(req)
      val files = m.getFilesByTransId(req)

      transfer match {
        case Some(xfer) => sender ! Some(Map("result" -> true, "transfer" -> xfer, "files" -> files, "donor" -> m.getDonorWeb(UUID.fromString(xfer.donorId))))
        case None => None
      } 
  }
}