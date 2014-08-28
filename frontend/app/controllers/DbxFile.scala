package controllers

import com.dropbox.core.{ DbxAppInfo, DbxRequestConfig, DbxWebAuthNoRedirect }
import java.util.{ UUID, Locale }
import models._
import play.api._
import play.api.libs.json._
import play.api.mvc._
import play.api.libs.ws._
import play.api.Play.current

object DbxFile extends Controller {
  val dbConfig = new DbxRequestConfig("DLTS", Locale.getDefault().toString)
  
  def show = Action {
  	Ok("file")
  }
}