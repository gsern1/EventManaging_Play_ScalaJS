package models



import slick.driver.H2Driver.api._
import sun.security.util.Password

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration


/**
 * A simple representation of a user.
 *
 * @author Antoine Drabble
  *
 */

class User(_username: String, _password: String) {
       var username = _username
       var password = _password
}

/*
case class User(username:String, password:String, id:Option[Int]= None)

class Users(tag:Tag) extends Table[User](tag, "USERS"){
    def id = column[Int]("USR_ID", O.PrimaryKey, O.AutoInc) // This is the primary key column
    def username = column[String]("USR_NAME")
    def password = column[String]("USR_PW")

    // Every table needs a * projection with the same type as the table's type parameter
    def * = (username, password, id.?) <> ((User.apply _).tupled, User.unapply)


}

object User{
    private val db = Database.forConfig("h2mem1")
    lazy val data = TableQuery[Users]

    def create(username: String, password: String): Option[String] = {
        val users = for(u <- Await.result(db.run(data.result), Duration.Inf)) yield u.username
        if(users.contains(username)) None
        else{
            val session = username + password
            Await.result(db.run(DBIO.seq(data += User(username, password))), Duration.Inf)
            println("added : " + (for (u <- Await.result(db.run(data.result), Duration.Inf)) yield u.username))
            Some(session)
        }
    }
}*/