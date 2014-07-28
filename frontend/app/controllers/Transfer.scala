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

object Transfer extends Controller {

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
          Transfers.insert(new Transfer(xferUUID, user.id.get, now, "Pending Review"))
          paths.foreach{path =>
            Files.insert(getFile(xferUUID, user.id.get, path, client))
          }
        }
        Redirect(routes.Donor.user)
      } case None => Redirect(routes.Donor.index)
    }
  }  


  def getFile(transUUID: UUID, userId: Long, path: String, client: DbxClient): File = {
    val md = client.getMetadata(path).asFile
    val path2 = path.substring(0, path.length - md.name.length)
    new File(UUID.randomUUID, userId, transUUID, md.name, path2, md.humanSize, md.numBytes, new java.sql.Date(md.lastModified.getTime), "Queued")
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

  def summarizeFiles(files: Vector[File]): Map[String, Tuple2[Int, Long]] = {
    import org.apache.commons.io.FilenameUtils
    var fileTypes = Map.empty[String, Tuple2[Int, Long]]

    files.foreach{ file =>
      val ext = FilenameUtils.getExtension(file.filename).toLowerCase
      if(fileTypes.contains(ext)){
        val currentFileType = fileTypes(ext)
        fileTypes = fileTypes - ext
        fileTypes = fileTypes ++ Map(ext -> new Tuple2(currentFileType._1 + 1, currentFileType._2 + file.size))
      } else {
        fileTypes = fileTypes ++ Map(ext -> new Tuple2(1, file.size))
      }
    }
    fileTypes
  }
}