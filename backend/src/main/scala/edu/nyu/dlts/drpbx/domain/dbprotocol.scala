package edu.nyu.dlts.drpbx.backend.domain

import java.util.UUID

object DBProtocol {
  case object Create
  case object Drop
  case object Purge
  case class Admin(id: UUID, name: String, password: String)
  case class Login(name: String, password: String)
}