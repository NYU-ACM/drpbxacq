package models

import play.api.Play.current
import scala.slick.driver.PostgresDriver.simple._
import scala.slick.lifted.Tag

case class User(id: Option[Long], email: String, passMd5: String, token:String)
case class Login(email: String, password: String)
case class Entry(filename: String, path: String, humanSize: String, size: Long, mDate: java.util.Date)

class Users(tag: Tag) extends Table[User](tag, "USER") {
  def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)
  def email = column[String]("EMAIL", O.NotNull)
  def passMd5 = column[String]("PASSMD5", O.NotNull)
  def token = column[String]("TOKEN", O.NotNull)
  def * = (id.?, email, passMd5, token) <> (User.tupled, User.unapply _)
}

object Users {
  val users = TableQuery[Users]

  def insert(user: User)(implicit s: Session) {
    users.insert(user)
  }

  def findById(uid: Long)(implicit s: Session): User = { 
    users.filter(_.id === uid).list.head
  }
  
  def count(implicit s: Session): Int = Query(users.length).first

  def validateLogin(email: String, hash: String)(implicit s: Session): Option[User] = {

    val user = users.filter(_.email === email).list.head
    val code = new String(new sun.misc.BASE64Encoder().encodeBuffer(hash.getBytes))

    if(code == user.passMd5) Some(user) else None
  }

}
