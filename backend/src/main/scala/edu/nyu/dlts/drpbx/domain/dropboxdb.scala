package edu.nyu.dlts.drpbx.backend.domain

import scala.slick.driver.PostgresDriver.simple._
import scala.slick.lifted.Tag
import java.util.{ UUID }
import org.apache.commons.codec.binary.Hex
import java.security.MessageDigest


trait Xfer {

  case class User(id: Option[Long], email: String, name: String, org: String, passMd5: String, token:String)

  class Users(tag: Tag) extends Table[User](tag, "USERS") {
    def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)
    def email = column[String]("EMAIL", O.NotNull)
    def name = column[String]("NAME", O.NotNull)
    def org = column[String]("ORG", O.NotNull)
    def passMd5 = column[String]("PASSMD5", O.NotNull)
    def token = column[String]("TOKEN", O.NotNull)

    def * = (id.?, email, name, org, passMd5, token) <> (User.tupled, User.unapply _)
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

    def getUserMap(implicit s: Session): Map[Long, String] = {
      var userMap = Map.empty[Long, String]
      for(user <- users){
        userMap += (user.id.get -> user.org) 
      }
      userMap
    }
    
    def count(implicit s: Session): Int = Query(users.length).first

    def validateLogin(email: String, password: String)(implicit s: Session): Option[User] = {
      val md5 = MessageDigest.getInstance("MD5").digest(password.getBytes)
      val code = new String(Hex.encodeHexString(md5))
      val user = users.filter(_.email === email).list.head
      if(code == user.passMd5) Some(user) else None
    }
  }

  case class Transfer(id: UUID, userId: Long, title: String, xferDate: java.sql.Date, status: Int, accessionId: String, adminNote: String, donorNote: String)

  class Transfers(tag: Tag) extends Table[Transfer](tag, "TRANSFERS") {
    def id = column[UUID]("ID", O.PrimaryKey)
    def title = column[String]("TITLE", O.NotNull)
    def userId = column[Long]("USER_ID", O.NotNull)
    def xferDate = column[java.sql.Date]("XFER_DATE", O.NotNull)
    def status = column[Int]("STATUS", O.NotNull)
    def accessionId = column[String]("ACCESSION_ID", O.NotNull)
    def adminNote = column[String]("ADMIN_NOTE", O.NotNull)
    def donorNote = column[String]("DONOR_NOTE", O.NotNull)
    def * = (id, userId, title, xferDate, status, accessionId, adminNote, donorNote) <> (Transfer.tupled, Transfer.unapply _)
    def user = foreignKey("USR_FK", userId, Transfers.users)(_.id)
  }

  object Transfers {
    val transfers = TableQuery[Transfers]
    val users = TableQuery[Users]

    def insert(transfer: Transfer)(implicit s: Session) {
      transfers.insert(transfer)
    }

    def getTransfersByUserId(id: Long)(implicit s: Session): Vector[Transfer] = {
      var xfers = Vector.empty[Transfer]
      transfers.filter(_.userId === id).list.foreach{ xfer => xfers = xfers ++ Vector(xfer) } 
      xfers
    }

    def getTransfersByStatus(id: Int)(implicit s: Session): Vector[Transfer] = {
      var xfers = Vector.empty[Transfer]
      transfers.filter(_.status === id).list.foreach{ xfer => 
        xfers = xfers ++ Vector(xfer) 
      }
      xfers
    }

    def findTransferById(id: UUID)(implicit s: Session): Transfer = {
      transfers.filter(_.id === id).list.head
    }

    def updateStatus(id: UUID, status: Int)(implicit s: Session) {
      val q = for { t <- transfers if t.id === id } yield t.status
      q.update(status)
      q.updateStatement
      q.updateInvoker
    }

    def updateAccessionNum(id: UUID, acc: String)(implicit s: Session) {
      val q = for { t <- transfers if t.id === id } yield t.accessionId
      q.update(acc)
      q.updateStatement
      q.updateInvoker
    }

      def updateAdminNote(id: UUID, note: String)(implicit s: Session) {
      val q = for { t <- transfers if t.id === id } yield t.adminNote
      q.update(note)
      q.updateStatement
      q.updateInvoker
    }
  }

  /*
   * Files
   */

  case class File(id: UUID, userId: Long, xferId: UUID, rev: String, filename: String, path: String, humanSize: String, size: Long, modDate: java.sql.Date, status: String)

  class Files(tag: Tag) extends Table[File](tag, "FILES") {
    def id = column[UUID]("ID", O.PrimaryKey)
    def userId = column[Long]("USER_ID", O.NotNull)
    def xferId = column[UUID]("TRANS_ID", O.NotNull)
    def rev = column[String]("REVISION", O.NotNull)
    def filename = column[String]("FILENAME", O.NotNull)
    def path = column[String]("PATH", O.NotNull)
    def humanSize = column[String]("HUMAN_SIZE", O.NotNull)
    def size = column[Long]("SIZE", O.NotNull)
    def modDate = column[java.sql.Date]("MOD_DATE", O.NotNull)
    def status = column[String]("STATUS", O.NotNull)
    def * = (id, userId, xferId, rev, filename, path, humanSize, size, modDate, status) <> (File.tupled, File.unapply _)
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

  case class Admin(id: UUID, email: String, passMd5: String)

  class Admins(tag: Tag) extends Table[Admin](tag, "ADMINS") {
    def id = column[UUID]("ID", O.PrimaryKey)
    def email = column[String]("EMAIL", O.NotNull)
    def passMd5 = column[String]("PASSMD5", O.NotNull)
    def * = (id, email, passMd5) <> (Admin.tupled, Admin.unapply _)
  }

  object Admins {

    val admins = TableQuery[Admins]
    val files = TableQuery[Files]
    val users = TableQuery[Users]
    val transfers = TableQuery[Transfers]

    def insert(admin: Admin)(implicit s: Session) {
      admins.insert(admin)
    }
    def validateLogin(email: String, hash: String)(implicit s: Session): Option[Admin] = {
      val admin = admins.filter(_.email === email).list.head
      val md5 = MessageDigest.getInstance("MD5").digest(hash.getBytes)
      val code = new String(Hex.encodeHexString(md5))
      if(code == admin.passMd5) Some(admin) else None
    }
  }
}