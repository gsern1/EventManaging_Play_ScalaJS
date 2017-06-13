package models


import javax.inject.Inject

import play.api.db.slick.DatabaseConfigProvider
import slick.driver.JdbcProfile

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Model for users
  *
  * @param id       the user's id
  * @param username the user's username
  * @param password the user's password
  */
case class User(id: Long, username: String, password: String)

/**
  * Repository for Users, used as a DAO
  *
  * @param eventRepo
  * @param dbConfigProvider
  */
class UserRepo @Inject()(eventRepo: EventRepo)(protected val dbConfigProvider: DatabaseConfigProvider) {


	val dbConfig = dbConfigProvider.get[JdbcProfile]
	val db = dbConfig.db

	import dbConfig.driver.api._

	private[models] val Users = TableQuery[UsersTable]

	private def _findById(id: Long): DBIO[Option[User]] =
		Users.filter(_.id === id).result.headOption

	private def _findByName(name: String): Query[UsersTable, User, List] =
		Users.filter(_.username === name).to[List]


	def findById(id: Long): Future[Option[User]] =
		db.run(_findById(id))

	def findByName(username: String): Future[Option[User]] =
		db.run(_findByName(username).result).map(_.headOption)

	def userExists(username: String): Future[Boolean] =
		findByName(username).map(_.isDefined)

	def allUsers: Future[List[User]] =
		db.run(Users.to[List].result)

	def registerUser(username: String, password: String): Future[Long] = {
		val user = User(0, username, password)
		db.run(Users returning Users.map(_.id) += user)
	}

	def updateUser(id: Long, username: String, password: String) = {
		val query = Users.filter(_.id === id).update(User(id, username, password))

		db.run(query)
	}

	def authenticate(username: String, password: String): Future[Boolean] = {
		findByName(username).filter(user => user.orNull != null && password.equals(user.orNull.password)).map(_.isDefined)
	}

	def delete(name: String): Future[Int] = {
		val query = _findByName(name)

		val interaction = for {

			users <- query.result
			userDeleted <- query.delete

		} yield userDeleted
		db.run(interaction.transactionally)
	}

	/**
	  * Definition of the users table properties
	  *
	  * @param tag
	  */
	private[models] class UsersTable(tag: Tag) extends Table[User](tag, "users") {

		def id = column[Long]("id", O.PrimaryKey, O.AutoInc) // This is the primary key column
		def username = column[String]("username")

		def password = column[String]("password")

		// Every table needs a * projection with the same type as the table's type parameter
		def * = (id, username, password) <> (User.tupled, User.unapply)

		def ? = (id.?, username.?, password.?).shaped.<>({ r => import r._; _1.map(_ => User.tupled((_1.get, _2.get, _3.get))) }, (_: Any) => throw new Exception("Inserting into ? projection not supported."))


	}

}

