package edu.nyu.dlts.drpbx.backend.domain

import scala.slick.driver.JdbcProfile
import scala.slick.jdbc.JdbcBackend.Database
import edu.nyu.dlts.drpbx.backend.domain.DBProtocol._
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID

class DBModel(name: String, dal: DAL, db: Database) {

  import dal._

  import dal.profile.simple._

  implicit val implicitSession = db.createSession

  def createDB = dal.create
  def dropDB = dal.drop
  def insertAdmin(admin: Admin) = dal.createAdmin(admin)
  def loginAdmin(login: Login): Option[Admin] = dal.validateAdminLogin(login)
  def insertDonor(donor: Donor) = dal.createDonor(donor)
  def loginDonor(login: Login): Option[Donor] = dal.validateDonorLogin(login)
  def getDonor(email: String): Option[Donor] = dal.getDonorByEmail(email)
  def getDonorWeb(id: UUID): Option[DonorWeb] = dal.getDonorById(id)
  def getDonorToken(req: TokenReq): Option[String] = dal.getTokenById(req)
  def insertTransfer(req: TransferReq): TransferResponse = dal.createTransfer(req)
  def getTransfers(): List[TransferWeb] = dal.getAllTransfers
  def getDonorTransfers(req: DonorTransfersReq): Option[List[TransferWeb]] = dal.getTransfersById(req)
  def getTransferById(req: TransferId): Option[TransferWeb] = dal.getTransfer(req)
  def getFilesByTransId(req: TransferId): List[FileWeb] = dal.getFilesByTransferId(req)
  def getFileById(req: FileReq): Option[FileWeb] = dal.getFile(req)
  def getDonorId(req: TransReq): Option[UUID] = dal.getDonorIdByTransferId(req)
}