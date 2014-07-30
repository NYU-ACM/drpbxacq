package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.db.slick._
import play.api.Play.current

import models._

import java.util.{ Locale, UUID }
import java.security.MessageDigest

object Admin extends Controller {
  def index = Action { implicit request => Ok(views.html.admin.login(loginForm)) }
  
  def login = Action { request =>  
    val email = request.body.asFormUrlEncoded.get("email").head
    val hash = request.body.asFormUrlEncoded.get("password").head
    Redirect(routes.Admin.validateLogin(email, hash))
  }

  def logout = Action { request =>
    Redirect(routes.Admin.index).withNewSession
  }

  def validateLogin(email: String, hash: String) = DBAction { implicit rs =>
    Admins.validateLogin(email, hash) match {
      case Some(user) => Redirect(routes.Admin.home).withSession("admin" -> "authorized", "email" -> user.email)
      case None => {
      	Redirect(routes.Admin.index).flashing("denied" -> "The Credentials Provided Could Not Be Validated")
      }
    }
  }

  def home = Action { request =>
    request.session.get("email") match {
      case Some(email) => {
        val pendingXfers = DB.withSession{ implicit session => Transfers.getTransfersByStatus(1) } 
        Ok(views.html.admin.home(pendingXfers))
      }

      case None => {
        Redirect(routes.Admin.index).flashing("denied" -> "You do not have a valid session, please login")  
      }
    }
  }

  def insert = Action { 
  	import org.apache.commons.codec.binary.Hex
  	val md5 = MessageDigest.getInstance("MD5").digest("password".getBytes)
  	val admin = new Admin(UUID.randomUUID, "admin", Hex.encodeHexString(md5))
  	DB.withSession{ implicit session => Admins.insert(admin) }
  	Redirect(routes.Admin.index)
  }

  val loginForm = Form(
    mapping(
      "email" -> nonEmptyText,
      "password" -> nonEmptyText
    )(Login.apply)(Login.unapply)
  )
}