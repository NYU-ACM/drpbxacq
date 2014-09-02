package models

import play.api.libs.json._
import play.api.libs.functional.syntax._
import java.util.UUID


case class DonorModel(id: String, email: String, name: String, org: String, passMd5: String, token:String)
case class Entry(filename: String, path: String, revision: String, humanSize: String, size: Long, mDate: java.util.Date)
case class Xfer(id: UUID, userId: UUID, title: String, xferDate: java.sql.Date, status: Int, accessionId: String, adminNote: String, donorNote: String)
case class XferWeb(id: String, donorId: String, title: String, xferDate: Long, status: Int, accessionId: Option[String], adminNote: Option[String], donorNote: Option[String], cancelNote: Option[String])
case class FileWeb(id: String, xferId: String, rev: String, filename: String, path: String, humanSize: String,size: Long, modDate: Long, status: Int)
case class DonorWeb(id: String, name: String, org: String)


trait JsonImplicits {

  implicit val donorReads: Reads[DonorWeb] = (
    (JsPath \ "id").read[String] and
    (JsPath \ "name").read[String] and
    (JsPath \ "org").read[String]
  )(DonorWeb.apply _)

  implicit val xferReads: Reads[XferWeb] = (
  	(JsPath \ "id").read[String] and
  	(JsPath \ "donorId").read[String] and
  	(JsPath \ "title").read[String] and
  	(JsPath \ "xferDate").read[Long] and
  	(JsPath \ "status").read[Int] and
  	(JsPath \ "accessionId").read[Option[String]] and
  	(JsPath \ "adminNote").read[Option[String]] and
  	(JsPath \ "donorNote").read[Option[String]] and
    (JsPath \ "cancelNote").read[Option[String]]
  )(XferWeb.apply _)

  implicit val fileReads: Reads[FileWeb] = (
    (JsPath \ "id").read[String] and
    (JsPath \ "xferId").read[String] and
    (JsPath \ "rev").read[String] and  
    (JsPath \ "filename").read[String] and
    (JsPath \ "path").read[String] and
    (JsPath \ "humanSize").read[String] and
    (JsPath \ "size").read[Long] and
    (JsPath \ "modDate").read[Long] and
    (JsPath \ "status").read[Int]
  )(FileWeb.apply _)
}

trait FileSummarySupport {  
  import org.apache.commons.io.FilenameUtils
  def summarizeFiles(files: List[FileWeb]): Map[String, Tuple2[Int, Long]] = {
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


/*
  implicit val donorReads: Reads[DonorModel] = (
    (JsPath \ "id").read[String] and
    (JsPath \ "email").read[String] and
    (JsPath \ "name").read[String] and
    (JsPath \ "org").read[String] and
    (JsPath \ "md5").read[String] and
    (JsPath \ "token").read[String]
  )(DonorModel.apply _)
  */