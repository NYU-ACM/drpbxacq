package edu.nyu.dlts.drpbx.backend.domain

import scala.slick.driver.PostgresDriver
import scala.slick.jdbc.JdbcBackend.Database
import com.typesafe.config._

trait DBConfig {
  val dbconf = ConfigFactory.load()
  val url = dbconf.getString("prodDb.url")
  val drver = dbconf.getString("prodDb.driver")
  val usr = dbconf.getString("prodDb.user")
  val passwd = dbconf.getString("prodDb.password")
  def m: DBModel
}

trait DrpbxDbSupport extends DBConfig {
  val db = Database.forURL(url, driver=drver, user=usr, password=passwd)
  val dal = new DAL(PostgresDriver)
  val m = new DBModel("postgresql", dal, db)
} 