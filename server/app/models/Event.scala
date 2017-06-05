package models

/**
  * Created by guillaume on 01.06.17.
  */


import java.util.Date
import javax.inject.Inject

import com.sun.org.glassfish.gmbal.Description
import play.api.db.slick.DatabaseConfigProvider
import slick.driver.JdbcProfile

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}



case class Event(id: Long, name: String, date: String, description: String, creator: Long) {

	def patch(name: Option[String], date: Option[String],  description: Option[String], creator: Option[Long]): Event =
		this.copy(name = name.getOrElse(this.name),
			date = date.getOrElse(this.date),
			description = description.getOrElse(this.description),
			creator = creator.getOrElse(this.creator))
}


class EventRepo @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) {

	val dbConfig = dbConfigProvider.get[JdbcProfile]
	val db = dbConfig.db
	import dbConfig.driver.api._

	private[models] val Events = TableQuery[EventsTable]


	def findById(id: Long): Future[Event] =
		db.run(Events.filter(_.id === id).result.head)


	def findByUserId(creatorId: Long): Future[List[Event]] =
		db.run(Events.filter(_.creator === creatorId).to[List].result)



	def partialUpdate(id: Long, name: Option[String], date: Option[String], description: Option[String], creator: Option[Long]): Future[Int] = {
		import scala.concurrent.ExecutionContext.Implicits.global

		val query = Events.filter(_.id === id)

		val update = query.result.head.flatMap {event =>
			query.update(event.patch(name, date, description, creator))
		}

		db.run(update)
	}

	def all(): DBIO[Seq[Event]] =
		Events.result

	def createEvent(name: String, date: String, description: String, creator: Long): Future[Long] = {

		//val creator = Await.result(userRepo.findByName(creatorName), Duration(10, "seconds")).head.id;

		val event = Event(0,name,date,description,creator)
		db.run(Events returning Events.map(_.id) += event)

	}


/*
	object DateMapper {

		val utilDate2SqlTimestampMapper = MappedColumnType.base[java.util.Date, java.sql.Timestamp](
			{ utilDate => new java.sql.Timestamp(utilDate.getTime()) },
			{ sqlTimestamp => new java.util.Date(sqlTimestamp.getTime()) })

		val utilDate2SqlDate = MappedColumnType.base[java.util.Date, java.sql.Date](
			{ utilDate => new java.sql.Date(utilDate.getTime()) },
			{ sqlDate => new java.util.Date(sqlDate.getTime()) })

	}*/

	private[models] class EventsTable(tag: Tag) extends Table[Event](tag, "events") {

		//implicit val dateColumnType = DateMapper.utilDate2SqlDate

		def id = column[Long]("id", O.AutoInc, O.PrimaryKey)
		def name = column[String]("name")
		def date = column[String]("date")//(DateMapper.utilDate2SqlDate.optionType)
		def description = column[String]("description")
		def creator = column[Long]("creator")

		def * = (id, name, date, description, creator) <> (Event.tupled, Event.unapply)
		def ? = (id.?, name.?, date.?, description.?, creator.?).shaped.<>({ r => import r._; _1.map(_ => Event.tupled((_1.get, _2.get, _3.get, _4.get, _5.get))) }, (_: Any) => throw new Exception("Inserting into ? projection not supported."))
	}


}