package models

import play.api.Play.current
import scala.slick.driver.PostgresDriver.simple._
import scala.slick.lifted.Tag
import java.util.{UUID}

/*
 * Misc. case classes
 */

case class Login(email: String, password: String)
case class Entry(filename: String, path: String, humanSize: String, size: Long, mDate: java.util.Date)


/*
 * Users
 */

case class User(id: Option[Long], email: String, passMd5: String, token:String)

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

  def findByEmail(email: String)(implicit s: Session): User = {
    users.filter(_.email === email).list.head

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

/*
 * Transfers
 */

case class Transfer(id: UUID, userId: Long, xferDate: java.sql.Date)

class Transfers(tag: Tag) extends Table[Transfer](tag, "TRANSFERS") {
  def id = column[UUID]("ID", O.PrimaryKey)
  def userId = column[Long]("USER_ID", O.NotNull)
  def xferDate = column[java.sql.Date]("XFER_DATE", O.NotNull)
  def * = (id, userId, xferDate) <> (Transfer.tupled, Transfer.unapply _)
  def user = foreignKey("USR_FK", userId, Transfers.users)(_.id)
}

object Transfers {
  val transfers = TableQuery[Transfers]
  val users = TableQuery[Users]

  def insert(transfer: Transfer)(implicit s: Session) {
    transfers.insert(transfer)
  }

  def getTransfersByUserId(id: Long)(implicit s: Session): Vector[UUID] = {
    var xfers = Vector.empty[UUID]
    transfers.filter(_.userId === id).list.foreach{xfer =>
      if(! xfers.contains(xfer.id)) xfers = xfers ++ Vector(xfer.id)
    } 
    xfers
  }
}

/*
 * Files
 */

case class File(id: UUID, userId: Long, xferId: UUID, filename: String, path: String, humanSize: String, size: Long, modDate: java.sql.Date, status: String)

class Files(tag: Tag) extends Table[File](tag, "FILES") {
  def id = column[UUID]("ID", O.PrimaryKey)
  def userId = column[Long]("USER_ID", O.NotNull)
  def xferId = column[UUID]("TRANS_ID", O.NotNull)
  def filename = column[String]("FILENAME", O.NotNull)
  def path = column[String]("PATH", O.NotNull)
  def humanSize = column[String]("HUMAN_SIZE", O.NotNull)
  def size = column[Long]("SIZE", O.NotNull)
  def modDate = column[java.sql.Date]("MOD_DATE", O.NotNull)
  def status = column[String]("STATUS", O.NotNull)
  def * = (id, userId, xferId, filename, path, humanSize, size, modDate, status) <> (File.tupled, File.unapply _)
  def user = foreignKey("USR_FK", userId, Files.users)(_.id)
  def xfer = foreignKey("XFR_FK", xferId, Files.transfers)(_.id)
}

object Files {
  val files = TableQuery[Files]
  val users = TableQuery[Users]
  val transfers = TableQuery[Transfers]

  def insert(file: File)(implicit s: Session) {
    files.insert(file)
  }



  def getFilesByTransferId(transferId: UUID)(implicit s: Session): Vector[File] = {
    var transferFiles = Vector.empty[File]
    files.filter(_.xferId === transferId).list.foreach{file =>
      transferFiles = transferFiles ++ Vector(file)
    }
    transferFiles
  }
}