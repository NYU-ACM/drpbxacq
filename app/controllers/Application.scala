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

import models._

object Application extends Controller {

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
    val hash = new String(MessageDigest.getInstance("MD5").digest(request.body.asFormUrlEncoded.get("password").head.getBytes))
    Redirect(routes.Application.validateLogin(email, hash))
  }

  def logout = Action { request =>
    Redirect(routes.Application.index).withNewSession
  }

  def validateLogin(email: String, hash: String) = DBAction { implicit rs =>
    Users.validateLogin(email, hash) match {
      case Some(user) => Redirect(routes.Application.user).withSession("token" -> user.token)
      case None => Redirect(routes.Application.index)
    }
  }

  def user = Action { request =>
    request.session.get("token") match {
      case Some(token) => {    
        val client = new DbxClient(dbConfig, token)
        val listing = client.getMetadataWithChildren("/")
        Ok(views.html.user(listing.children, listing.entry))
      }
      case None => {
        Redirect(routes.Application.index)
      } 
    }
  }

  def save = DBAction { implicit rs =>
    keyForm.bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.create(keyForm, url)),
      user => {
        val md5 = new String(MessageDigest.getInstance("MD5").digest(user.passMd5.getBytes))
        val code = new String(new sun.misc.BASE64Encoder().encodeBuffer(md5.getBytes))
        val user2 = new User(user.id, user.email, code, webAuth.finish(user.token).accessToken)        
        Users.insert(user2)
        Redirect(routes.Application.user).withSession("token" -> user2.token)
      }
    )
  }

  def entry(path: String) = Action{ request =>
    val decodedPath = new String(new sun.misc.BASE64Decoder().decodeBuffer(path))
    val client = new DbxClient(dbConfig, request.session.get("token").get)
    val entries = getEntryMap(decodedPath, client)
    Ok(views.html.entry(decodedPath, entries))
  }


  def getEntryMap(path: String, client: DbxClient): Map[String, Vector[Entry]] = {
    import scala.collection.JavaConversions._
    var dirs = Map.empty[String, Vector[Entry]]
    val listing = client.getMetadataWithChildren(path)
    var files = Vector.empty[Entry]
    
    listing.children.foreach{ entry => 
      if(entry.isFile){
        val e = new Entry(entry.asFile.name, entry.asFile.path, entry.asFile.humanSize, entry.asFile.numBytes, entry.asFile.lastModified) 
        files = files ++ Vector(e)
       }   
      
      if(entry.isFolder){
        dirs = dirs ++ getEntryMap(entry.path, client)   
      }
    }
    
    if(!files.isEmpty) dirs = dirs ++ Map(path -> files)
    dirs
  }

  /**
   *Form for key
   */
  
  val keyForm = Form(
    mapping(
      "id" -> optional(longNumber),
      "email" -> nonEmptyText,
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
