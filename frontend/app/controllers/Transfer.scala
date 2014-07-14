package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.db.slick._
import play.api.Play.current
import play.api.libs.json._

import com.dropbox.core._
import java.util.{UUID,Locale}
import java.security.MessageDigest

import models._

object Transfer extends Controller {

  val key = Play.current.configuration.getString("drpbx.key").get
  val secret = Play.current.configuration.getString("drpbx.secret").get
  val appInfo = new DbxAppInfo(key, secret)
  val dbConfig = new DbxRequestConfig("DLTS", Locale.getDefault().toString)
  
  def transfer = Action { request =>
    val transUUID = UUID.randomUUID
    val client = new DbxClient(dbConfig, request.session.get("token").get)
    var files = Vector.empty[File]
    request.body.asFormUrlEncoded match {
      case Some(form) => {
        println(form.get("files[]").get)
        form.get("files[]").get.foreach{path =>
          files = files ++ Vector(getFile(transUUID, path, client))
        }
          Ok(views.html.transfer(files))
        }
      case None => Redirect(routes.Application.index)
    } 
  }

  def getFile(transUUID: UUID, path: String, client: DbxClient): File = {
    val md = client.getMetadata(path).asFile
    new File(UUID.randomUUID, transUUID, md.name, path, md.humanSize, md.numBytes, new java.sql.Date(md.lastModified.getTime), "Queued")
  }

  def entry(path: String) = Action{ request =>
    request.session.get("token") match {
      case Some(s) => {
        val decodedPath = new String(new sun.misc.BASE64Decoder().decodeBuffer(path))
        val client = new DbxClient(dbConfig, request.session.get("token").get)
        val entries = getEntryMap(decodedPath, client)
        Ok(views.html.entry(decodedPath, entries))
      }
      case None => Redirect(routes.Application.index)
    }
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
}