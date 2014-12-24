package edu.nyu.dlts.drpbx.backend.domain

import java.util.UUID

object DBProtocol {
  case object Create
  case object Drop
  case object Purge
  case object TransferAll
  //db models
  case class Admin(id: UUID, name: String, password: String)
  case class Login(name: String, password: String)
  case class Donor(id: UUID, email: String, name: String, org: String, passMd5: String, token:String)
  case class DonorWeb(id: String, name: String, org: String)
  case class Transfer(id: UUID, donorId: UUID, title: String, xferDate: java.sql.Date, status: Int, accessionId: Option[String], adminNote: Option[String], donorNote: Option[String], cancelNote: Option[String])
  case class TransferWeb(id: String, donorId: String, title: String, xferDate: Long, status: Int, accessionId: Option[String], adminNote: Option[String], donorNote: Option[String], cancelNote: Option[String])
  case class File(id: UUID, xferId: UUID, rev: String, filename: String, path: String, humanSize: String, size: Long, modDate: java.sql.Date, status: Int)
  case class FileWeb(id: String, xferId: String, rev: String, filename: String, path: String, humanSize: String,size: Long, modDate: Long, status: Int)
  
  //protocols
  case class EmailReq(email: String)
  case class TokenReq(id: UUID)
  case class TransferReq(donorId: String, title: String, donorNote: String, date: Long, paths: Seq[String])
  case class TransferApproveReq(transferId: String, accessionId: String, adminNote: String)
  case class TransferCancelReq(transferId: String, adminNote: String)
  case class TransferId(id: UUID)
  case class TransferStatusUpdate(id:UUID, status: Int)
  case class TransferResponse(result: Boolean, count: Int)
  case class DonorTransfersReq(id: UUID)
  case class FileReq(id: UUID)
  case class FileDLReq(id: UUID)
  case class DonorReq(id: UUID)
  case class TransReq(id: UUID)
  case class FileDownloadReq(fileId: UUID, client: com.dropbox.core.DbxClient)
}