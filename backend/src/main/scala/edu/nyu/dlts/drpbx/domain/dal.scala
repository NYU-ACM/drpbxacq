package edu.nyu.dlts.drpbx.backend.domain

import scala.slick.driver.JdbcProfile
import scala.slick.backend.DatabaseComponent
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import scala.slick.driver.PostgresDriver.simple._
import scala.slick.driver.JdbcProfile
import java.util.{ Locale, UUID }
import com.dropbox.core.{ DbxRequestConfig, DbxClient }


trait Profile {
  val profile: JdbcProfile
}

class DAL(override val profile: JdbcProfile) extends DrpbxAcq with Profile {
  import profile.simple._
  import edu.nyu.dlts.drpbx.backend.domain.DBProtocol._
  val dbConfig = new DbxRequestConfig("DLTS", Locale.getDefault().toString)
  val logger: Logger = LoggerFactory.getLogger("drpbx.domain")


  def createDB(implicit s: Session) {
  	create
  }

  def dropDB(implicit s: Session) {
  	drop
  }

  def createAdmin(admin: Admin)(implicit s: Session) {
    admins.insert(admin)
  }

  def validateAdminLogin(login: Login)(implicit s: Session): Option[Admin] = {
	  val admin = admins.filter(_.email === login.name).list
    if(admin.isEmpty) None
    else if(login.password == admin.head.password) Some(admin.head) else None
  }

  def createDonor(donor: Donor)(implicit s: Session) {
    donors.insert(donor)
  }

  def validateDonorLogin(login: Login)(implicit s: Session): Option[Donor] = {
    val donor = donors.filter(_.email === login.name).list
    if(donor.isEmpty) None
    else if(login.password == donor.head.passMd5) Some(donor.head) else None
  }

  def getDonorByEmail(email: String)(implicit s: Session): Option[Donor] = {
    val donor = donors.filter(_.email === email).list
    if(donor.isEmpty) None
    else Some(donor.head) 
  }

  def getDonorById(id: UUID)(implicit s: Session): Option[DonorWeb] = {
    val donor = donors.filter(_.id === id).list
    if(donor.isEmpty) None 
    else Some(new DonorWeb(donor.head.id.toString, donor.head.name, donor.head.org))
  }

  def getTokenById(req: TokenReq)(implicit s: Session): Option[String] = {
    val donor = donors.filter(_.id === req.id).list
    if(donor.isEmpty) None else Some(donor.head.token)
  }

  def getDonorIdByTransferId(req: TransReq)(implicit s: Session): Option[UUID] = {
    val trans = transfers.filter(_.id === req.id).list
    if(trans.isEmpty) None else Some(trans.head.donorId)
  }

  def createTransfer(req: TransferReq)(implicit s: Session): TransferResponse = {  
    val xferId = UUID.randomUUID
    val date = new java.sql.Date(req.date)
    val transfer = new Transfer(xferId, UUID.fromString(req.donorId), req.title,  date, 1, None, None, Some(req.donorNote))
    transfers.insert(transfer)
    logger.info(transfer.title + " CREATED")

    val token = getTokenById(new TokenReq(transfer.donorId)).get
    val client = new DbxClient(dbConfig, token)

    var count = 0;

    req.paths.foreach{ path => 
      val md = client.getMetadata(path).asFile
      val path2 = path.substring(0, path.length - md.name.length)
      val file = new File(UUID.randomUUID, xferId, md.rev, md.name, path2, md.humanSize, md.numBytes, new java.sql.Date(md.lastModified.getTime), "Queued")
      files.insert(file)
      count = count + 1
      logger.info(file.filename + " ADDED")
    }
    new TransferResponse(true, count)
  }

  def getAllTransfers()(implicit s: Session): List[TransferWeb] = {
    var xfers = List.empty[TransferWeb]
    for(transfer <- transfers){
      val xfer = new TransferWeb(transfer.id.toString, transfer.donorId.toString, transfer.title, transfer.xferDate.getTime, transfer.status, transfer.accessionId, transfer.adminNote, transfer.donorNote)
      xfers = xfers ++ List(xfer)
    }
    xfers
  }  

  def getTransfersById(req: DonorTransfersReq)(implicit s: Session): Option[List[TransferWeb]] = {
    var xfers = List.empty[TransferWeb]
    val trans = transfers.filter(_.donorId === req.id).list
    if(trans.isEmpty) None 
    else {
      for(transfer <- trans){
        val xfer = new TransferWeb(transfer.id.toString, transfer.donorId.toString, transfer.title, transfer.xferDate.getTime, transfer.status, transfer.accessionId, transfer.adminNote, transfer.donorNote)
        xfers = xfers ++ List(xfer)
      }
      Some(xfers)
    }
  }

  def getTransfer(req: TransferId)(implicit s: Session): Option[TransferWeb] = {
    val trans = transfers.filter(_.id === req.id).list
    if(trans.isEmpty) None
    else{
      val transfer = trans.head 
      Some(new TransferWeb(transfer.id.toString, transfer.donorId.toString, transfer.title, transfer.xferDate.getTime, transfer.status, transfer.accessionId, transfer.adminNote, transfer.donorNote))
    }
  }

  def getFilesByTransferId(req: TransferId)(implicit s: Session): List[FileWeb] = {
    var f = List.empty[FileWeb]
    val fileList = files.filter(_.xferId === req.id).list
    if(fileList.isEmpty) None
    for(file <- fileList) {
        f = f ++ List(new FileWeb(file.id.toString, file.xferId.toString, file.rev, file.filename, file.path, file.humanSize, file.size, file.modDate.getTime, file.status))
    }
    f
  }

  def getFile(req: FileReq)(implicit s: Session): Option[FileWeb] = {
    val fileList = files.filter(_.id === req.id).list
    if(fileList.isEmpty) None
    else{
      val file = fileList.head
      Some(new FileWeb(file.id.toString, file.xferId.toString, file.rev, file.filename, file.path, file.humanSize, file.size, file.modDate.getTime, file.status))   
    }
  }

  def approveTransfer(req: TransferApproveReq)(implicit s: Session): Boolean = {
    var q = for { t <- transfers if t.id === UUID.fromString(req.transferId) } yield (t.status, t.accessionId, t.adminNote)
    q.update(2, Some(req.accessionId), Some(req.accessionId))
    true
  }
}