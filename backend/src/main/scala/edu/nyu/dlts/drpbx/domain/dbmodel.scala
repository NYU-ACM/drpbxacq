package edu.nyu.dlts.drpbx.backend.domain

import scala.slick.driver.JdbcProfile
import scala.slick.jdbc.JdbcBackend.Database
import edu.nyu.dlts.drpbx.backend.domain.DBProtocol._
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class DBModel(name: String, dal: DAL, db: Database) {

  import dal._

  import dal.profile.simple._

  implicit val implicitSession = db.createSession

  def createDB = dal.create
  def dropDB = dal.drop
  def insertAdmin(admin: Admin) = dal.createAdmin(admin)
  def loginAdmin(login: Login): Option[Admin] = dal.validateLogin(login)
}