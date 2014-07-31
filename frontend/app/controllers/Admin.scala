package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.db.slick._
import play.api.Play.current

import models._
import helpers._ 

import java.util.{ Locale, UUID }
import java.security.MessageDigest

object Admin extends Controller with FileHelper {
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
    request.session.get("admin") match {
      case Some(email) => {
        val pendingXfers = DB.withSession{ implicit session => Transfers.getTransfersByStatus(1) } 
        val activeXfers = DB.withSession{ implicit session => Transfers.getTransfersByStatus(2) } 
        val userMap = DB.withSession { implicit session => Users.getUserMap }   
        Ok(views.html.admin.home(pendingXfers, activeXfers, userMap))
      }

      case None => {
        Redirect(routes.Admin.index).flashing("denied" -> "You do not have a valid session, please login")  
      }
    }
  }

  def transfer(uuid: String) = Action { request => 
    request.session.get("admin") match {
      case Some(email) => {
        val transfer = DB.withSession { implicit session => Transfers.findTransferById(UUID.fromString(uuid))}
        val user = DB.withSession { implicit session => Users.findById(transfer.userId)}
        val files = DB.withSession{ implicit session => Files.getFilesByTransferId(UUID.fromString(uuid))}
        val summary = summarizeFiles(files)

        Ok(views.html.admin.transfer(transfer, user, files, summary))
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

  def approve(uuid: String) = Action { request =>
    request.session.get("admin") match {
      case Some(email) => {
        DB.withSession { implicit session => Transfers.updateStatus(UUID.fromString(uuid), 2)}
        Ok("You're going to be OK")
      }

      case None => Redirect(routes.Admin.index).flashing("denied" -> "You do not have a valid session, please login")   
    }
  }

  val loginForm = Form(
    mapping(
      "email" -> nonEmptyText,
      "password" -> nonEmptyText
    )(Login.apply)(Login.unapply)
  )
}