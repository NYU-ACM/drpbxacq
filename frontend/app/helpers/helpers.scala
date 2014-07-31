package helpers

import models._
import org.apache.commons.io.FilenameUtils


trait FileHelper {  

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