package edu.nyu.dlts.drpbx

import org.scalatra._
import org.scalatra.json._
import org.json4s.{ DefaultFormats, Formats }
import _root_.akka.actor.{ Actor, ActorRef, ActorSystem }
import edu.nyu.dlts.drpbx.backend.domain._

class Backend(system: ActorSystem ) extends DrpbxBackendStack with JacksonJsonSupport {
  protected implicit val jsonFormats: Formats = DefaultFormats

  before() {
    contentType = formats("json")
  }

  get("/") {
    "DONE"
  }
}