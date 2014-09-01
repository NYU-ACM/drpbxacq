package edu.nyu.dlts.drpbx.backend.serializers

import edu.nyu.dlts.drpbx.backend.domain.DBProtocol._
import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.json4s.JsonDSL._
import _root_.akka.actor.{ Actor, ActorRef, ActorSystem, Props }
import _root_.akka.pattern.ask
import _root_.akka.util.Timeout

trait Serializers {
  class XferSerializer extends CustomSerializer[TransferWeb](format => ({
    case jv: JValue =>
      val id = (jv \ "id").extract[String]
      val donorId = (jv \ "id").extract[String]
      val title = (jv \ "title").extract[String]
      val xferDate = (jv \ "xferDate").extract[Long]
      val status = (jv \ "status").extract[Int]
      val accessionId = (jv \ "accessionId").extract[Option[String]]
      val adminNote = (jv \ "adminNote").extract[Option[String]]
      val donorNote = (jv \ "donorNote").extract[Option[String]]
      TransferWeb(id, donorId, title, xferDate, status, accessionId, adminNote, donorNote)
  }, { case xfer: TransferWeb => 
      ("id" -> xfer.id) ~ 
      ("donorId" -> xfer.donorId) ~
      ("title" -> xfer.title) ~
      ("xferDate" -> xfer.xferDate) ~
      ("status" -> xfer.status) ~    
      ("accessionId" -> xfer.accessionId.getOrElse(null)) ~ 
      ("adminNote" -> xfer.adminNote.getOrElse(null)) ~ 
      ("donorNote" -> xfer.donorNote.getOrElse(null)) 
	 }))

  protected implicit val jsonFormats: Formats = DefaultFormats + new XferSerializer

}