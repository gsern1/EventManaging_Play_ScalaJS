package models

/**
  * Created by guillaume on 01.06.17.
  */


import javax.inject.Inject

import play.api.db.slick.DatabaseConfigProvider
import slick.driver.JdbcProfile

import scala.concurrent.Future

/**
  * Repository to record the participation of an user to an event
  *
  * @param id      the id of the participation record
  * @param eventID the event's id
  * @param userID  the user's id
  */
case class EventParticipant(id: Long, eventID: Long, userID: Long) {

	def patch(id: Option[Long], eventID: Option[Long], userID: Option[Long]): EventParticipant =
		this.copy(id = id.getOrElse(this.id),
			eventID = eventID.getOrElse(this.eventID),
			userID = userID.getOrElse(this.userID))
}

/**
  * Repository for participation, used as a DAO
  *
  * @param eventRepo
  * @param userRepo
  * @param dbConfigProvider
  */
class EventParticipantRepo @Inject()(eventRepo: EventRepo, userRepo: UserRepo)(protected val dbConfigProvider: DatabaseConfigProvider) {
	val dbConfig = dbConfigProvider.get[JdbcProfile]
	val db = dbConfig.db

	import dbConfig.driver.api._

	private[models] val EventParticipants = TableQuery[EventParticipantTable]

	def createParticipation(eventID: Long, userID: Long): Future[Long] = {
		val participation = EventParticipant(0, eventID, userID)
		db.run(EventParticipants returning EventParticipants.map(_.id) += participation)
	}

	def deleteParticipation(eventID: Long, userID: Long): Future[Int] = {
		db.run(EventParticipants.filter(ep => ep.eventID === eventID && ep.userID === userID).delete)
	}

	def findByEventIdAndUserId(eventID: Long, userID: Long): Future[List[EventParticipant]] =
		db.run(EventParticipants.filter(ep => ep.eventID === eventID && ep.userID === userID).to[List].result)

	def findByEventId(eventID: Long): Future[List[EventParticipant]] =
		db.run(EventParticipants.filter(_.eventID === eventID).to[List].result)

	def findByUserId(userID: Long): Future[List[EventParticipant]] =
		db.run(EventParticipants.filter(_.userID === userID).to[List].result)

	/**
	  * Definition of the event_participant table properties
	  *
	  * @param tag
	  */
	private[models] class EventParticipantTable(tag: Tag) extends Table[EventParticipant](tag, "event_participant") {

		def id = column[Long]("id", O.AutoInc, O.PrimaryKey)

		def eventID = column[Long]("event_id")

		def userID = column[Long]("user_id")

		def * = (id, eventID, userID) <> (EventParticipant.tupled, EventParticipant.unapply)

		def ? = (id.?, eventID.?, userID.?).shaped.<>({ r => import r._; _1.map(_ => EventParticipant.tupled((_1.get, _2.get, _3.get))) }, (_: Any) => throw new Exception("Inserting into ? messages not supported."))
	}


}