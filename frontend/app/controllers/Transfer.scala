package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.db.slick._
import play.api.Play.current

import com.dropbox.core._
import java.util.{UUID,Locale}
import java.security.MessageDigest

import models._
import helpers._ 

object Transfer extends Controller with FileHelper {

  val key = Play.current.configuration.getString("drpbx.key").get
  val secret = Play.current.configuration.getString("drpbx.secret").get
  val appInfo = new DbxAppInfo(key, secret)
  val dbConfig = new DbxRequestConfig("DLTS", Locale.getDefault().toString)
  
  def transfer = Action { request =>
    val paths = request.body.asFormUrlEncoded.get("files[]")

    request.session.get("email") match {
      case Some(email) => {
        val user = DB.withSession{ implicit session => Users.findByEmail(email)}
        val xferUUID = UUID.randomUUID
        val client = new DbxClient(dbConfig, user.token)
        var files = Vector.empty[File]
        val now = new java.sql.Date(new java.util.Date().getTime())
        DB.withSession{ implicit session =>
          Transfers.insert(new Transfer(xferUUID, user.id.get, request.body.asFormUrlEncoded.get("xferName")(0), now, 1, ""))
          paths.foreach{path =>
            Files.insert(getFile(xferUUID, user.id.get, path, client))
          }
        }
        Redirect(routes.Donor.user).flashing{"success" -> ("Transfer " + xferUUID + " Created")}
      } case None => Redirect(routes.Donor.index)
    }
  }  


  def getFile(transUUID: UUID, userId: Long, path: String, client: DbxClient): File = {
    val md = client.getMetadata(path).asFile
    val path2 = path.substring(0, path.length - md.name.length)
    new File(UUID.randomUUID, userId, transUUID, md.rev, md.name, path2, md.humanSize, md.numBytes, new java.sql.Date(md.lastModified.getTime), "Queued")
  }

  def entry(path: String) = Action{ request =>
    request.session.get("email") match {
      case Some(email) => {
        val user = DB.withSession{ implicit session => Users.findByEmail(email)}
        val decodedPath = new String(new sun.misc.BASE64Decoder().decodeBuffer(path))
        val client = new DbxClient(dbConfig, user.token)
        val entries = getEntryMap(decodedPath, client)
        Ok(views.html.entry(decodedPath, entries))
      }
      case None => Redirect(routes.Donor.index)
    }
  }

  def getEntryMap(path: String, client: DbxClient): Map[String, Vector[Entry]] = {
    import scala.collection.JavaConversions._
    var dirs = Map.empty[String, Vector[Entry]]
    val listing = client.getMetadataWithChildren(path)
    var files = Vector.empty[Entry]
    
    listing.children.foreach{ entry => 
      if(entry.isFile){
        val e = new Entry(entry.asFile.name, entry.asFile.path, entry.asFile.rev, entry.asFile.humanSize, entry.asFile.numBytes, entry.asFile.lastModified) 
        files = files ++ Vector(e)
       }   
      
      if(entry.isFolder){
        dirs = dirs ++ getEntryMap(entry.path, client)   
      }
    }
    
    if(!files.isEmpty) dirs = dirs ++ Map(path -> files)
    dirs
  }

  def show(uuid: String) = Action{ request =>
    request.session.get("email") match {
      case Some(email) => {
        val user = DB.withSession{ implicit session => Users.findByEmail(email)}
        val transfer = DB.withSession { implicit session => Transfers.findTransferById(UUID.fromString(uuid))}
        val files = DB.withSession{ implicit session => Files.getFilesByTransferId(UUID.fromString(uuid))}
        val summary = summarizeFiles(files)
        Ok(views.html.show(user, transfer, summary, files))
      }  
      case None => Redirect(routes.Donor.index)
    }
  }


}