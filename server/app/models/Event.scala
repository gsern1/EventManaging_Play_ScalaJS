package models

/**
  * Created by guillaume on 01.06.17.
  */



import javax.inject.Inject

import play.api.db.slick.DatabaseConfigProvider
import slick.driver.JdbcProfile
import java.sql.Timestamp

import scala.concurrent.{Await, Awaitable, Future}



case class Event(id: Long, name: String, date: Timestamp, location:String, description: String, creator: Long, picture : Option[Long]) {

	def patch(name: Option[String], date: Option[Timestamp],  location: Option[String],  description: Option[String], creator: Option[Long], picture : Option[Long] ): Event =
		this.copy(name = name.getOrElse(this.name),
			date = date.getOrElse(this.date),
			location = location.getOrElse(this.location),
			description = description.getOrElse(this.description),
			creator = creator.getOrElse(this.creator),
			picture = picture)
}


class EventRepo @Inject()(pictureRepo: PictureRepo)(protected val dbConfigProvider: DatabaseConfigProvider) {
  val dbConfig = dbConfigProvider.get[JdbcProfile]
	val db = dbConfig.db
	import dbConfig.driver.api._

	private[models] val Events = TableQuery[EventsTable]


	def findById(id: Long): Future[Event] =
		db.run(Events.filter(_.id === id).result.head)


	def findByUserId(creatorId: Long): Future[List[Event]] =
		db.run(Events.filter(_.creator === creatorId).to[List].result)

	def findAll(): Future[List[Event]] =
		db.run(Events.to[List].result)



	def partialUpdate(id: Long, name: Option[String], date: Option[Timestamp], location: Option[String],description: Option[String], creator: Option[Long], picture:Option[Long]): Future[Int] = {
		import scala.concurrent.ExecutionContext.Implicits.global

		val query = Events.filter(_.id === id)

		val update = query.result.head.flatMap {event =>
			query.update(event.patch(name, date, location, description, creator, picture))
		}

		db.run(update)
	}

	def all(): DBIO[Seq[Event]] =
		Events.result

	def createEvent(name: String, date: Timestamp, location:String, description: String, creator: Long, picture: Option[Long]): Future[Long] = {

		val event = Event(0,name,date,location,description,creator,picture)
		db.run(Events returning Events.map(_.id) += event)

	}

	def updateEvent(id: Long, name: String, date: Timestamp, location:String, description: String, creator: Long, picture: Option[Long]) = {
		val query = Events.filter(_.id === id).update(Event(id, name, date, location, description, creator, picture))

		db.run(query)
	}

	private[models] class EventsTable(tag: Tag) extends Table[Event](tag, "events") {

		//implicit val dateColumnType = DateMapper.utilDate2SqlDate

		def id = column[Long]("id", O.AutoInc, O.PrimaryKey)
		def name = column[String]("name")
		def date = column[java.sql.Timestamp]("date")
		def location = column[String]("location")
		def description = column[String]("description")
		def creator = column[Long]("creator")
		def picture = column[Option[Long]]("picture")

		def * = (id, name, date, location, description, creator, picture) <> (Event.tupled, Event.unapply)
	}


}