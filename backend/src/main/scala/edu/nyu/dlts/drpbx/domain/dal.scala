package edu.nyu.dlts.drpbx.backend.domain

import scala.slick.driver.JdbcProfile
import scala.slick.backend.DatabaseComponent
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import scala.slick.driver.PostgresDriver.simple._
import scala.slick.driver.JdbcProfile
import edu.nyu.dlts.drpbx.backend.domain.DBProtocol._

trait Profile {
  val profile: JdbcProfile
}

class DAL(override val profile: JdbcProfile) extends DrpbxAcq with Profile {
  import profile.simple._

  val logger: Logger = LoggerFactory.getLogger("drpbx.domain");
  logger.info("Model class instantiated")

  def createDB(implicit s: Session) {
  	create
  }

  def dropDB(implicit s: Session) {
  	drop
  }
  
}