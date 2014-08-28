package controllers

import models._

import play.api._
import play.api.libs.json._
import play.api.mvc._
import play.api.libs.ws._
import play.api.data._
import play.api.data.Forms._
import play.api.Play.current
import scala.concurrent.Future
import com.dropbox.core.{ DbxAppInfo, DbxRequestConfig, DbxWebAuthNoRedirect }
import java.util.{ UUID, Locale }
import models._

object Donor extends Controller with JsonImplicits with FileSummarySupport {

  val key = Play.current.configuration.getString("drpbx.key").get
  val secret = Play.current.configuration.getString("drpbx.secret").get
  val appInfo = new DbxAppInfo(key, secret)
  val dbConfig = new DbxRequestConfig("DLTS", Locale.getDefault().toString)
  val webAuth = new DbxWebAuthNoRedirect(dbConfig, appInfo)
  val url = webAuth.start
  implicit val context = play.api.libs.concurrent.Execution.Implicits.defaultContext
  
  def login = Action { implicit request =>
  	Ok(views.html.donor.login())
  }

  def create = Action {    
    Ok(views.html.donor.create(keyForm, url))
  }

  def save = Action.async { implicit request =>
    val form = request.body.asFormUrlEncoded
    val url = s"http://localhost:8080/donor/create"
    val token = webAuth.finish(form.get("token").head).accessToken
    val backendRequest = 
      Map(
        "requestId" -> Seq(UUID.randomUUID.toString),
        "email" -> Seq(form.get("email").head),
        "password" -> Seq(form.get("password").head),
        "org" -> Seq(form.get("org").head),
        "name" -> Seq(form.get("name").head),
        "token" -> Seq(token)
      )
      
    WS.url(url).post(backendRequest).map { response => 
      val jsonResponse: JsValue = response.json
      val result: JsBoolean = (jsonResponse \ "result").as[JsBoolean]
      result.value match {
        case true => Redirect(routes.Donor.index).flashing("info" -> ("Successfully registered " + (jsonResponse \ "name").toString) )
        case false => Redirect(routes.Donor.index).flashing("error" -> "Cold not register account")
      }
    }
  }

  def validate = Action.async { implicit request =>
    val form = request.body.asFormUrlEncoded
    val email = form.get("email").head 
    val password = form.get("password").head
    val url = s"http://localhost:8080/donor/login?email=$email&password=$password"

    WS.url(url).get.map { response =>
      val jsonResponse: JsValue = response.json
      val result: JsBoolean = (jsonResponse \ "result").as[JsBoolean]
      result.value match {
        case true => Redirect(routes.Donor.index).withSession("id" -> (jsonResponse \ "id").as[JsString].value)
        case false => Redirect(routes.Donor.login).flashing("denied" -> "Credentials could not be validated.")
      }
    }
  }

  def logout = Action { request =>
    Redirect(routes.Donor.login).withNewSession.flashing("info" -> "Succesfully Logged out")
  }

  def index = Action.async { implicit request =>
    request.session.get("id") match {
      case Some(id) => {
        var pendingXfers = Vector.empty[XferWeb]
        WS.url(s"http://localhost:8080/donor/$id/transfers").get.map { response =>
          val json: JsValue = response.json
          val result: JsBoolean = (json \ "result").as[JsBoolean]
          result.value match {
            case true => {
              
              (json \ "transfers").as[List[XferWeb]].foreach{ xfer =>
                if(xfer.status == 1) pendingXfers = pendingXfers ++ Vector(xfer)
              }
              
              Ok(views.html.donor.index(pendingXfers))
            }
            case false =>  Redirect(routes.Donor.index)
          }
        }
      }
      case None => Future.successful(Redirect(routes.Donor.login).flashing("denied" -> "You do not have a valid session, please login."))
    } 
  }

  def transfer(transferId: String) = Action.async { implicit request =>
    request.session.get("id") match {
      case Some(id) => {
       WS.url(s"http://localhost:8080/transfer/$transferId").get.map { response =>
          val json: JsValue = response.json
          val result: JsBoolean = (json \ "result").as[JsBoolean]
          result.value match {
            case true => { 
              val transfer = (json \ "transfer").as[XferWeb]
              val files = (json \ "files").as[List[FileWeb]]
              val donor = (json \ "donor").as[DonorWeb]
              val summary = summarizeFiles(files)
              Ok(views.html.donor.transfer(transfer, files, donor, summary)) 
            }
            case false =>  Redirect(routes.Donor.index)
          }
        }
      } case None => Future.successful(Redirect(routes.Donor.login).flashing("denied" -> "You do not have a valid session, please login."))
    }
  }

  val keyForm = Form(
    mapping(
      "id" -> nonEmptyText,
      "email" -> nonEmptyText,
      "name" -> nonEmptyText,
      "org" -> nonEmptyText,
      "password" -> nonEmptyText, 
      "token" -> nonEmptyText
    )(DonorModel.apply)(DonorModel.unapply)
  )
}