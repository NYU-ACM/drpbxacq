package controllers

import play.api._
import play.api.libs.json._
import play.api.mvc._
import play.api.libs.ws._
import play.api.Play.current
import scala.concurrent.Future
import scala.util.{ Success, Failure }
import models._
object Admin extends Controller with JsonImplicits {
  
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
        WS.url("http://localhost:8080/transfer/all").get.map { response =>
      	  val json: JsValue = response.json
          val result: JsBoolean = (json \ "result").as[JsBoolean]
          result.value match {
            case true => {
              (json \ "transfers").as[List[XferWeb]].foreach{ xfer =>
                if(xfer.status == 1) pendingXfers = pendingXfers ++ Vector(xfer)
              }
              Ok(views.html.admin.index(pendingXfers))
            }
            case false => Ok("ko")
          }
        }
      }
      case None => Future.successful(Redirect(routes.Admin.login).flashing("denied" -> "You do not have a valid admin session, please login."))
    }
  }
}