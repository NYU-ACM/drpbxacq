package controllers

import play.api._
import play.api.data._
import play.api.data.Forms._
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.libs.ws._
import play.api.mvc._
import play.api.Play.current

import scala.concurrent.Future
import scala.concurrent.duration._

import java.util.{ Locale, UUID }
import com.dropbox.core.{ DbxClient, DbxRequestConfig }

import models._

object Transfer extends Controller with JsonImplicits {
  val dbConfig = new DbxRequestConfig("DLTS", Locale.getDefault().toString)
  implicit val context = play.api.libs.concurrent.Execution.Implicits.defaultContext

  def index = Action.async { implicit request =>
    request.session.get("id") match {
      case Some(id) => {
        val url = s"http://localhost:8080/donor/$id/token"
        WS.url(url).get.map { response =>
          val jsonResponse: JsValue = response.json
          val result: JsBoolean = (jsonResponse \ "result").as[JsBoolean]
          result.value match {
            case true => {
              val token = (jsonResponse \ "token").as[JsString].value
              val client = new DbxClient(dbConfig, token)
              val listing = client.getMetadataWithChildren("/")
              Ok(views.html.transfer.index(listing.children, listing.entry))
            } case false => Ok("ko")
          }   
        }
      } case None => Future.successful(Redirect(routes.Donor.index).flashing("denied" -> "You do not have a valid session, please login."))
    }
  }

  def entry(path: String) = Action.async { implicit request =>
    request.session.get("id") match {
      case Some(id) => {
        val url = s"http://localhost:8080/donor/$id/token"
        WS.url(url).get.map { response =>
          val jsonResponse: JsValue = response.json
          val result: JsBoolean = (jsonResponse \ "result").as[JsBoolean]
          result.value match {
            case true => {
              val token = (jsonResponse \ "token").as[JsString].value
              val decodedPath = new String(new sun.misc.BASE64Decoder().decodeBuffer(path))
              val entries = getEntryMap(decodedPath, new DbxClient(dbConfig, token))
              Ok(views.html.transfer.entry(decodedPath, entries))
            } case false => Ok("ko")
          }
        }
      } case None => Future.successful(Redirect(routes.Donor.index).flashing("denied" -> "You do not have a valid session, please login."))
    }
  }

  def save = Action.async { implicit request =>
    request.session.get("id") match {
      case Some(id) => {
        
        val form = request.body.asFormUrlEncoded 
        val now = new java.sql.Date(new java.util.Date().getTime())
        val paths = form.get("files[]")
        var seq = Seq.empty[String]
        val title = form.get("xferName")(0)
        for(path <- paths){ seq = seq ++ Seq(path)}
        val jsonRequest = Json.obj(
          "donorId" -> id, 
          "title" -> title, 
          "date" -> now, 
          "donorNote" -> form.get("donorNote")(0), 
          "paths" -> Json.toJson(seq)
        )

        //
        val url = s"http://localhost:8080/transfer"
        WS.url(url).post(jsonRequest).map { response => 
          val jsonResponse: JsValue = response.json
          val result: JsBoolean = (jsonResponse \ "result").as[JsBoolean]
          result.value match { 
            case true => { 
              val count = (jsonResponse \ "count").as[JsNumber].toString
              Redirect(routes.Donor.index()).flashing("success" -> s"$title transferred [$count files]") 
            }
            case false => Ok("ko")
          }
        }
      } case None => Future.successful(Redirect(routes.Donor.index).flashing("denied" -> "You do not have a valid session, please login."))
    }
  }

  def approve = Action.async { implicit request =>
    request.session.get("admin") match {
      case Some(admin) => {
        val form = request.body.asFormUrlEncoded
        val id = form.get("transferId").head
        val jsonRequest = Json.obj("transferId" -> id, "accessionId" -> form.get("accessionId").head, "adminNote" -> form.get("adminNote").head)
        
        WS.url(s"http://localhost:8080/transfer/approve").post(jsonRequest).map { response => 
          val json: JsValue = response.json
          val result: JsBoolean = (json \ "result").as[JsBoolean]
          result.value match { 
            case true => { Redirect(routes.Admin.index).flashing("success" -> s"Transfer $id aprroved") }
            case false => Ok("ko")
          }
        }
      }
      case None => Future.successful(Redirect(routes.Admin.index).flashing("denied" -> "You do not have a valid session, please login."))
    }
  }

  def cancel = Action.async { implicit request =>
    request.session.get("admin") match {
      case Some(admin) => {
        val form = request.body.asFormUrlEncoded
        val id = form.get("transferId").head
        val jsonRequest = Json.obj("transferId" -> id, "adminNote" -> form.get("adminNote").head)
        WS.url(s"http://localhost:8080/transfer/cancel").post(jsonRequest).map { response =>
          val json: JsValue = response.json
          (json \ "result").as[JsBoolean].value match {
            case true => { Redirect(routes.Admin.index).flashing("success" -> s"Transfer $id cancelled") }
            case false => Ok("ko")
          }
        }
      } case None => Future.successful(Redirect(routes.Admin.index).flashing("denied" -> "You do not have a valid session, please login."))
    }
  }

  def download(transferId: String) = Action.async { implicit request =>
    request.session.get("admin") match {
      case Some(admin) => {
        WS.url(s"http://localhost:8080/transfer/$transferId/download").get.map { response =>
          val json: JsValue = response.json
          val result: JsBoolean = (json \ "result").as[JsBoolean]
          result.value match {
            case true => { Redirect(routes.Admin.index).flashing("success" -> s"Transfer $transferId download started.") } 
            case false => { Redirect(routes.Admin.index).flashing("failure" -> s"Transfer $transferId download not started.") }
          }
        }
      } 
      case None => Future.successful(Redirect(routes.Admin.index).flashing("denied" -> "You do not have a valid session, please login."))
    }
  }

  def getEntryMap(path: String, client: DbxClient): Map[String, Vector[Entry]] = {
    import scala.collection.JavaConversions._
    var dirs = Map.empty[String, Vector[Entry]]
    val listing = client.getMetadataWithChildren(path)
    var files = Vector.empty[Entry]
    
    listing.children.foreach{ entry => 
      if(entry.isFile){
        val e = new Entry(entry.asFile.name, entry.asFile.path, entry.asFile.rev, entry.asFile.humanSize, entry.asFile.numBytes, entry.asFile.lastModified) 
        files = files ++ Vector(e)
       }   
      
      if(entry.isFolder){
        dirs = dirs ++ getEntryMap(entry.path, client)   
      }
    }
    
    if(!files.isEmpty) dirs = dirs ++ Map(path -> files)
    dirs
  }
}