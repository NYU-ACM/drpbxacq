package edu.nyu.dlts.drpbx.backend.domain

import scala.slick.driver.PostgresDriver.simple._
import scala.slick.lifted.Tag
import java.util.{ UUID }
import org.apache.commons.codec.binary.Hex
import java.security.MessageDigest
import edu.nyu.dlts.drpbx.backend.domain.DBProtocol._


trait DrpbxAcq {


  val admins = TableQuery[Admins]
  val files = TableQuery[Files]
  val donors = TableQuery[Donors]
  val transfers = TableQuery[Transfers]

  def create(implicit session: Session): Unit = {
    (donors.ddl ++ transfers.ddl ++ files.ddl ++ admins.ddl).create
  }

  def drop(implicit session: Session): Unit = {
    (donors.ddl ++ transfers.ddl ++ files.ddl ++ admins.ddl).drop
  }

  class Donors(tag: Tag) extends Table[Donor](tag, "DONORS") {
    def id = column[UUID]("ID", O.PrimaryKey)
    def email = column[String]("EMAIL", O.NotNull)
    def name = column[String]("NAME", O.NotNull)
    def org = column[String]("ORG", O.NotNull)
    def passMd5 = column[String]("PASSMD5", O.NotNull)
    def token = column[String]("TOKEN", O.NotNull)

    def * = (id, email, name, org, passMd5, token) <> (Donor.tupled, Donor.unapply _)
  }

  class Transfers(tag: Tag) extends Table[Transfer](tag, "TRANSFERS") {
    def id = column[UUID]("ID", O.PrimaryKey)
    def donorId = column[UUID]("DONOR_ID", O.NotNull)
    def title = column[String]("TITLE", O.NotNull)
    def xferDate = column[java.sql.Date]("XFER_DATE", O.NotNull)
    def status = column[Int]("STATUS", O.NotNull)
    def accessionId = column[String]("ACCESSION_ID", O.NotNull)
    def adminNote = column[String]("ADMIN_NOTE", O.NotNull)
    def donorNote = column[String]("DONOR_NOTE", O.NotNull)
    def * = (id, donorId, title, xferDate, status, accessionId, adminNote, donorNote) <> (Transfer.tupled, Transfer.unapply _)
    def donor = foreignKey("DNR_FK", donorId, donors)(_.id)
  }


  class Files(tag: Tag) extends Table[File](tag, "FILES") {
    def id = column[UUID]("ID", O.PrimaryKey)
    def xferId = column[UUID]("TRANS_ID", O.NotNull)
    def rev = column[String]("REVISION", O.NotNull)
    def filename = column[String]("FILENAME", O.NotNull)
    def path = column[String]("PATH", O.NotNull)
    def humanSize = column[String]("HUMAN_SIZE", O.NotNull)
    def size = column[Long]("SIZE", O.NotNull)
    def modDate = column[java.sql.Date]("MOD_DATE", O.NotNull)
    def status = column[String]("STATUS", O.NotNull)
    def * = (id, xferId, rev, filename, path, humanSize, size, modDate, status) <> (File.tupled, File.unapply _)
    def xfer = foreignKey("XFR_FK", xferId, transfers)(_.id)
  }
  

  class Admins(tag: Tag) extends Table[Admin](tag, "ADMINS") {
    def id = column[UUID]("ID", O.PrimaryKey)
    def email = column[String]("EMAIL", O.NotNull)
    def passMd5 = column[String]("PASSMD5", O.NotNull)
    def * = (id, email, passMd5) <> (Admin.tupled, Admin.unapply _)
  }

}