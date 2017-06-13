package models

/**
  * Created by guillaume on 01.06.17.
  */



import javax.inject.Inject

import play.api.db.slick.DatabaseConfigProvider
import slick.driver.JdbcProfile
import java.sql.Timestamp

import scala.concurrent.{Await, Awaitable, Future}


/**
  * Model for messages
  * @param id the messages id
  * @param value the messages content
  * @param date the messages date
  * @param creator the messages creator
  * @param event the event in which the message was sent
  */
case class Message(id: Long, value: String, date: Timestamp, creator: Long, event : Long) {
	def patch(value: Option[String]): Message =
		this.copy(value = this.value)
}

/**
  * Repository for Messages, used as a DAO
  * @param eventRepo
  * @param userRepo
  * @param dbConfigProvider
  */
class MessageRepo @Inject()(eventRepo: EventRepo, userRepo: UserRepo)(protected val dbConfigProvider: DatabaseConfigProvider) {

	val dbConfig = dbConfigProvider.get[JdbcProfile]
	val db = dbConfig.db
	import dbConfig.driver.api._

	private[models] val Messages = TableQuery[MessagesTable]


	def findById(id: Long): Future[Message] =
		db.run(Messages.filter(_.id === id).result.head)


	def findByUserId(creatorId: Long): Future[List[Message]] =
		db.run(Messages.filter(_.creator === creatorId).to[List].result)

	def findByEvent(id: Long): Future[List[Message]] =
		db.run(Messages.filter(_.event === id).to[List].result)

	def findAll(): Future[List[Message]] =
		db.run(Messages.to[List].result)

	def all(): DBIO[Seq[Message]] =
		Messages.result

	def createMessage(value: String, creator: Long, event: Long): Future[Long] = {
		val message = Message(0,value,new Timestamp(System.currentTimeMillis()), creator, event)
		db.run(Messages returning Messages.map(_.id) += message)
	}

	def updateEvent(id: Long, value: String, creator: Long, event: Long)= {
		val query = Messages.filter(_.id === id).update(Message(0,value,new Timestamp(System.currentTimeMillis()), creator, event))

		db.run(query)
	}

	/**
	  * Definition of the messages table properties
	  * @param tag
	  */
	private[models] class MessagesTable(tag: Tag) extends Table[Message](tag, "messages") {

		//implicit val dateColumnType = DateMapper.utilDate2SqlDate

		def id = column[Long]("id", O.AutoInc, O.PrimaryKey)
		def value = column[String]("value")
		def date = column[java.sql.Timestamp]("date")
		def creator = column[Long]("creator")
		def event = column[Long]("event")

		def * = (id, value, date, creator, event) <> (Message.tupled, Message.unapply)
		def ? = (id.?, value.?, date.?, creator.?, event.?).shaped.<>({ r => import r._; _1.map(_ => Message.tupled((_1.get, _2.get, _3.get, _4.get, _5.get))) }, (_: Any) => throw new Exception("Inserting into ? messages not supported."))
	}


}