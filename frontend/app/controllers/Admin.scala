package controllers

import play.api._
import play.api.libs.json._
import play.api.mvc._
import play.api.libs.ws._
import play.api.Play.current
import scala.concurrent.Future
import scala.collection.SortedMap
import scala.util.{ Success, Failure }
import models._

object Admin extends Controller with JsonImplicits with FileSummarySupport {
  
  implicit val context = play.api.libs.concurrent.Execution.Implicits.defaultContext

  def login = Action { implicit request => Ok(views.html.admin.login())}
  
  def validateLogin = Action.async { implicit request => 	
  	val email = request.body.asFormUrlEncoded.get("email").head	
  	val password = request.body.asFormUrlEncoded.get("password").head
    val url = s"http://localhost:8080/admin/login?email=$email&password=$password"
    
    WS.url(url).get().map { response => 
      val json: JsValue = response.json
      val result: JsBoolean = (json \ "result").as[JsBoolean]
      result.value match {
      	case true => Redirect(routes.Admin.index).withSession("admin" -> "authorized", "email" -> email)
      	case false => Redirect(routes.Admin.login).flashing("denied" -> "Credentials could not be validated.")
      }
   	}
  }

  def logout = Action { request =>
    Redirect(routes.Admin.login).withNewSession.flashing("info" -> "Succesfully Logged out")
  }

  def index = Action.async { implicit request =>
    request.session.get("admin") match {
      case Some(admin) => { 
        var pendingXfers = Vector.empty[XferWeb]
        var activeXfers = Vector.empty[XferWeb]
        var completeXfers = Vector.empty[XferWeb]
        var cancelledXfers = Vector.empty[XferWeb]

        WS.url("http://localhost:8080/transfers").get.map { response =>
          val json: JsValue = response.json
          val result: JsBoolean = (json \ "result").as[JsBoolean]
          result.value match {
            case true => {
              (json \ "transfers").as[List[XferWeb]].foreach{ xfer =>
                if(xfer.status == 1) pendingXfers = pendingXfers ++ Vector(xfer)
                else if(xfer.status == 2) activeXfers = activeXfers ++ Vector(xfer)
              }
              Ok(views.html.admin.index(SortedMap(1 -> pendingXfers, 2 -> activeXfers, 3 -> completeXfers, 4 -> cancelledXfers)))
            }
            case false => Ok("ko")
          }
        } 
      }
      case None => Future.successful(Redirect(routes.Admin.login).flashing("denied" -> "You do not have a valid admin session, please login."))
    }
  }

  def transfer(transferId: String) = Action.async { implicit request =>
    request.session.get("admin") match {
      case Some(admin) => {
       WS.url(s"http://localhost:8080/transfer/$transferId").get.map { response =>
          val json: JsValue = response.json
          val result: JsBoolean = (json \ "result").as[JsBoolean]
          result.value match {
            case true => { 
              val transfer = (json \ "transfer").as[XferWeb]
              val files = (json \ "files").as[List[FileWeb]]
              val donor = (json \ "donor").as[DonorWeb]
              val summary = summarizeFiles(files)
              Ok(views.html.admin.transfer(transfer, files, donor, summary)) 
            }
            case false =>  Redirect(routes.Admin.index)
          }
        }
      } case None => Future.successful(Redirect(routes.Donor.login).flashing("denied" -> "You do not have a valid session, please login."))
    }
  }  
}