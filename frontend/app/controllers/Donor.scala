package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.db.slick._
import play.api.Play.current


import com.dropbox.core._
import java.util.Locale
import java.security.MessageDigest
import org.apache.commons.codec.binary.Hex

import models._

object Donor extends Controller {

  val key = Play.current.configuration.getString("drpbx.key").get
  val secret = Play.current.configuration.getString("drpbx.secret").get
  val appInfo = new DbxAppInfo(key, secret)
  val dbConfig = new DbxRequestConfig("DLTS", Locale.getDefault().toString)
  val webAuth = new DbxWebAuthNoRedirect(dbConfig, appInfo)
  val url = webAuth.start

  def index = Action { Ok(views.html.login(loginForm)) }

  def create = Action {    
    Ok(views.html.create(keyForm, url))
  }

  def login = Action { request =>  
    val email = request.body.asFormUrlEncoded.get("email").head
    val hash = request.body.asFormUrlEncoded.get("password").head
    Redirect(routes.Donor.validateLogin(email, hash))
  }

  def logout = Action { request =>
    Redirect(routes.Donor.index).withNewSession
  }

  def validateLogin(email: String, hash: String) = DBAction { implicit rs =>
    Users.validateLogin(email, hash) match {
      case Some(user) => Redirect(routes.Donor.user).withSession("email" -> user.email)
      case None => Redirect(routes.Donor.index)
    }
  }

  def user = Action { request =>
    request.session.get("email") match {
      
      case Some(email) => { 
        val user = DB.withSession{ implicit session => Users.findByEmail(email) }
        val transfers = DB.withSession{ implicit session => Transfers.getTransfersByUserId(user.id.get) }
        Ok(views.html.home(transfers, user))
      }

      case None => Redirect(routes.Donor.index)
    }
  }

  def transfer = Action { request =>
    request.session.get("email") match {
      case Some(email) => {
        val user = DB.withSession{ implicit session => Users.findByEmail(email)}
        val client = new DbxClient(dbConfig, user.token)
        val listing = client.getMetadataWithChildren("/")
        Ok(views.html.user(listing.children, listing.entry))
      } 
      case None => Redirect(routes.Donor.index)
    } 
  }

  def save = DBAction { implicit rs =>
    keyForm.bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.create(keyForm, url)),
      user => {
        val md5 = MessageDigest.getInstance("MD5").digest(user.passMd5.getBytes)
        val code = new String(Hex.encodeHexString(md5))
        val user2 = new User(user.id, user.email, user.name, user.org, code, webAuth.finish(user.token).accessToken)        
        Users.insert(user2)
        Redirect(routes.Donor.user).withSession("token" -> user2.token)
      }
    )
  }

  /**
   *Form for key
   */
  
  val keyForm = Form(
    mapping(
      "id" -> optional(longNumber),
      "email" -> nonEmptyText,
      "name" -> nonEmptyText,
      "org" -> nonEmptyText,
      "password" -> nonEmptyText, 
      "token" -> nonEmptyText
    )(User.apply)(User.unapply)
  )

  val loginForm = Form(
    mapping(
      "email" -> nonEmptyText,
      "password" -> nonEmptyText
    )(Login.apply)(Login.unapply)
  )
}
