package controllers

import com.dropbox.core.{ DbxClient, DbxRequestConfig }
import java.util.{ UUID, Locale }
import models._
import play.api._
import play.api.libs.json._
import play.api.mvc._
import play.api.libs.ws._
import play.api.Play.current
import scala.concurrent.Future

import java.io.{ File, FileOutputStream }

object DbxFile extends Controller with JsonImplicits {
  val dbConfig = new DbxRequestConfig("DLTS", Locale.getDefault().toString)
  implicit val context = play.api.libs.concurrent.Execution.Implicits.defaultContext

  def show(fileId: String) = Action.async { implicit request =>
  	request.session.get("admin") match {
  	  case Some(admin) => {
  	  	WS.url(s"http://localhost:8080/file/$fileId/download").get.map { response =>
  	      val json: JsValue = response.json
          val result: JsBoolean = (json \ "result").as[JsBoolean]
  	  	  result.value match {
            case true => { 
              val filemd = (json \ "file").as[FileWeb]
              val file = filemd.path + filemd.filename
              val dl = new File("/tmp", filemd.filename)
              dl.createNewFile
              val fos = new FileOutputStream(dl)
              val client = new DbxClient(dbConfig, (json \ "token").as[JsString].value)
              val dlFile = client.getFile(file, null, fos);    
              Ok.sendFile(
    			content = dl,
    			fileName = _ => filemd.filename
  			  )
            }
            case false => Ok("ok")
      	  }
      	}
      } case None => Future.successful(Ok("ko"))
  	}
  }
}