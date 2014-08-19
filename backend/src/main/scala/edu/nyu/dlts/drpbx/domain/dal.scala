package edu.nyu.dlts.drpbx.backend.domain

import scala.slick.driver.JdbcProfile
import scala.slick.backend.DatabaseComponent
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import scala.slick.driver.PostgresDriver.simple._
import scala.slick.driver.JdbcProfile


trait Profile {
  val profile: JdbcProfile
}

class DAL(override val profile: JdbcProfile) extends DrpbxAcq with Profile {
  import profile.simple._
  import edu.nyu.dlts.drpbx.backend.domain.DBProtocol._

  val logger: Logger = LoggerFactory.getLogger("drpbx.domain");
  logger.info("Model class instantiated")

  def createDB(implicit s: Session) {
  	create
  }

  def dropDB(implicit s: Session) {
  	drop
  }

  def createAdmin(admin: Admin)(implicit s: Session) {
    admins.insert(admin)
  }

  def validateLogin(login: Login)(implicit s: Session): Option[Admin] = {
  	  val admin = admins.filter(_.email === login.name).list.head
      if(login.password == admin.password) Some(admin) else None
  }
}