package models

/**
  * Created by guillaume on 01.06.17.
  */



import javax.inject.Inject

import play.api.db.slick.DatabaseConfigProvider
import slick.driver.JdbcProfile
import java.sql.Timestamp

import scala.concurrent.{Await, Future}



case class Event(id: Long, name: String, date: Timestamp, location:String, description: String, creator: Long, picture : Long) {

	def patch(name: Option[String], date: Option[Timestamp],  location: Option[String],  description: Option[String], creator: Option[Long], picture : Option[Long] ): Event =
		this.copy(name = name.getOrElse(this.name),
			date = date.getOrElse(this.date),
			location = location.getOrElse(this.location),
			description = description.getOrElse(this.description),
			creator = creator.getOrElse(this.creator),
			picture = picture.getOrElse(this.picture))
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

	def createEvent(name: String, date: Timestamp, location:String, description: String, creator: Long, picture: Long): Future[Long] = {

		//val creator = Await.result(userRepo.findByName(creatorName), Duration(10, "seconds")).head.id;

		//val format : DateTimeFormatter = DateTimeFormat.forPattern("YYYY-MM-DD HH:MM")
		//val datetime : Date = format(date)

		val event = Event(0,name,date,location,description,creator,picture)
		db.run(Events returning Events.map(_.id) += event)

	}





	private[models] class EventsTable(tag: Tag) extends Table[Event](tag, "events") {

		//implicit val dateColumnType = DateMapper.utilDate2SqlDate

		def id = column[Long]("id", O.AutoInc, O.PrimaryKey)
		def name = column[String]("name")
		def date = column[java.sql.Timestamp]("date")
		def location = column[String]("location")
		def description = column[String]("description")
		def creator = column[Long]("creator")
		def picture = column[Long]("picture")

		def * = (id, name, date, location, description, creator, picture) <> (Event.tupled, Event.unapply)
		def ? = (id.?, name.?, date.?, location.?, description.?, creator.?, picture.?).shaped.<>({ r => import r._; _1.map(_ => Event.tupled((_1.get, _2.get, _3.get, _4.get, _5.get, _6.get, _7.get))) }, (_: Any) => throw new Exception("Inserting into ? projection not supported."))
	}


}