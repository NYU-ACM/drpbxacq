package edu.nyu.dlts.drpbx.backend.domain

import scala.slick.driver.JdbcProfile
import scala.slick.jdbc.JdbcBackend.Database

import org.slf4j.Logger
import org.slf4j.LoggerFactory

class DBModel(name: String, dal: DAL, db: Database) {

  import dal._

  import dal.profile.simple._

  implicit val implicitSession = db.createSession

  def createDB = dal.create
  def dropDB = dal.drop

}