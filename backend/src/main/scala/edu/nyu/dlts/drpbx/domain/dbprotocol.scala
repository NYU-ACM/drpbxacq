package edu.nyu.dlts.drpbx.backend.domain

object DBProtocol {
  case object Create
  case object Drop
  case object Purge
  case class InsertAdminreate(name: String, password: String)
}