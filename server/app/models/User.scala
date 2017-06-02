package models


import javax.inject.Inject

import com.sun.xml.internal.ws.developer.UsesJAXBContext
import play.api.db.slick.DatabaseConfigProvider
import play.api.mvc.Result
import slick.dbio
import slick.dbio.Effect.Read
import slick.driver.JdbcProfile
import sun.security.util.Password

import scala.concurrent.ExecutionContext._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}


/**
 * A simple representation of a user.
 *
 * @author Antoine Drabble
  *
 */
/*
class User(_username: String, _password: String) {
       var username = _username
       var password = _password
}
*/

case class User(id: Long, username:String, password:String)

class UserRepo @Inject()(eventRepo: EventRepo)(protected val dbConfigProvider: DatabaseConfigProvider) {

    val dbConfig = dbConfigProvider.get[JdbcProfile]
    val db = dbConfig.db
    import dbConfig.driver.api._

    private[models] val Users = TableQuery[UsersTable]

    private def _findById(id: Long):DBIO[Option[User]] =
        Users.filter(_.id === id).result.headOption

    private def _findByName(name: String): Query[UsersTable, User, List] =
        Users.filter(_.username === name).to[List]



    def findById(id: Long): Future[Option[User]] =
        db.run(_findById(id))

    def findByName(username: String): Future[List[User]] =
        db.run(_findByName(username).result)

    def userExists(username:String): Future[Boolean]=
        findByName(username).map(_.headOption).map(_.isDefined)

    def allUsers: Future[List[User]] =
        db.run(Users.to[List].result)

    def registerUser(username: String, password:String): Future[Long] = {
        val user = User(0,username,password)
        db.run(Users returning Users.map(_.id) += user)
    }

    def authenticate(username: String, password: String)(implicit ec: ExecutionContext): Future[Boolean] = {
        findByName(username).filter( user => password == user.head.password).map(_.headOption).map(_.isDefined)
    }

    def delete(name:String): Future[Int] = {
        val query = _findByName(name)

        val interaction = for {

            users <- query.result
            userDeleted <- query.delete

        }yield userDeleted
        db.run(interaction.transactionally)
    }

    private [models] class UsersTable(tag:Tag) extends Table[User](tag, "USERS"){

        def id = column[Long]("USR_ID", O.PrimaryKey, O.AutoInc) // This is the primary key column
        def username = column[String]("USR_NAME")
        def password = column[String]("USR_PW")

        // Every table needs a * projection with the same type as the table's type parameter
        def * = (id, username, password) <> (User.tupled, User.unapply)
        def ? = (id.?, username.?, password.?).shaped.<>({ r => import r._; _1.map(_ => User.tupled((_1.get, _2.get, _3.get))) }, (_: Any) => throw new Exception("Inserting into ? projection not supported."))


    }

}

