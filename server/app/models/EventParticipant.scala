package models

/**
  * Created by guillaume on 01.06.17.
  */



import javax.inject.Inject

import play.api.db.slick.DatabaseConfigProvider
import slick.driver.JdbcProfile
import java.sql.Timestamp

import com.sun.java.accessibility.util.EventID

import scala.concurrent.{Await, Awaitable, Future}



case class EventParticipant(eventID: Long, userID: Long ) {

	def patch(eventID: Option[Long], userID: Option[Long]): EventParticipant =
		this.copy(eventID = eventID.getOrElse(this.eventID),
			userID = userID.getOrElse(this.userID))
}


class EventParticipantRepo @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) {
	val dbConfig = dbConfigProvider.get[JdbcProfile]
	val db = dbConfig.db

	import dbConfig.driver.api._

	private[models] val EventParticipants = TableQuery[EventParticipantTable]

	def createParticipation(eventID: Long, userID: Long): Future[(Long,Long)] = {

		val participation = EventParticipant(eventID, userID)
		db.run(EventParticipants returning EventParticipants.map(ep => (ep.eventID,ep.userID)) += participation)

	}

	def deleteParticipation(eventID: Long, userID: Long): Future[Int] = {

		db.run(EventParticipants.filter(ep => ep.eventID === eventID && ep.userID === userID).delete)

	}

	def findByEventIdAndUserId(eventID: Long, userID:Long): Future[List[EventParticipant]] =
		db.run(EventParticipants.filter(ep => ep.eventID === eventID && ep.userID === userID).to[List].result)

	def findByEventId(eventID: Long): Future[List[EventParticipant]] =
		db.run(EventParticipants.filter(_.eventID === eventID).to[List].result)

	def findByUserId(userID: Long): Future[List[EventParticipant]] =
		db.run(EventParticipants.filter(_.userID === userID).to[List].result)


	private[models] class EventParticipantTable(tag: Tag) extends Table[EventParticipant](tag, "event_participant") {

		def eventID: Rep[Long] = column[Long]("event_id")
		def userID: Rep[Long] = column[Long]("user_id")

		def * = (eventID, userID) <> (EventParticipant.tupled, EventParticipant.unapply)
		def ? = (eventID.?, userID.?).shaped.<>({ r => import r._; _1.map(_ => EventParticipant.tupled((_1.get, _2.get))) }, (_: Any) => throw new Exception("Inserting into ? messages not supported."))


		def pk = primaryKey("primaryKey", (eventID,userID))

		/*

		def eventFK = foreignKey("FK_EVENTS",eventID, TableQuery[Event])(recording =>
			recording.id , onDelete=ForeignKeyAction.Cascade)

		def userFK = foreignKey("FK_EVENTS",eventID, TableQuery[Event])(recording =>
			recording.id , onDelete=ForeignKeyAction.Cascade)*/

	}


}