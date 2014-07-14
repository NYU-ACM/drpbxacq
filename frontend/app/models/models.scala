package models

import play.api.Play.current
import scala.slick.driver.PostgresDriver.simple._
import scala.slick.lifted.Tag
import java.util.{UUID}

case class User(id: Option[Long], email: String, passMd5: String, token:String)
case class Login(email: String, password: String)
case class File(id: UUID, transferId: UUID, filename: String, path: String, humanSize: String, size: Long, modDate: java.sql.Date, status: String)
case class Entry(filename: String, path: String, humanSize: String, size: Long, mDate: java.util.Date)

class Users(tag: Tag) extends Table[User](tag, "USERS") {
  def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)
  def email = column[String]("EMAIL", O.NotNull)
  def passMd5 = column[String]("PASSMD5", O.NotNull)
  def token = column[String]("TOKEN", O.NotNull)
  def * = (id.?, email, passMd5, token) <> (User.tupled, User.unapply _)
}

object Users {
  val users = TableQuery[Users]

  def insert(user: User)(implicit s: Session) {
    users.insert(user)
  }

  def findById(uid: Long)(implicit s: Session): User = { 
    users.filter(_.id === uid).list.head
  }
  
  def count(implicit s: Session): Int = Query(users.length).first

  def validateLogin(email: String, hash: String)(implicit s: Session): Option[User] = {

    val user = users.filter(_.email === email).list.head
    val code = new String(new sun.misc.BASE64Encoder().encodeBuffer(hash.getBytes))

    if(code == user.passMd5) Some(user) else None
  }
}

class Files(tag: Tag) extends Table[File](tag, "FILES") {
  def id = column[UUID]("ID", O.PrimaryKey)
  def transId = column[UUID]("TRANS_ID", O.NotNull)
  def filename = column[String]("FILENAME", O.NotNull)
  def path = column[String]("PATH", O.NotNull)
  def humanSize = column[String]("HUMAN_SIZE", O.NotNull)
  def size = column[Long]("SIZE", O.NotNull)
  def modDate = column[java.sql.Date]("MOD_DATE", O.NotNull)
  def status = column[String]("STATUS", O.NotNull)
  def * = (id, transId, filename, path, humanSize, size, modDate, status) <> (File.tupled, File.unapply _)
}

object Files {
  val files = TableQuery[Files]

  def insert(file: File)(implicit s: Session) {
    files.insert(file)
  }
}