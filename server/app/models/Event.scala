package models

/**
  * Created by guillaume on 01.06.17.
  */


import javax.inject.Inject

import play.api.db.slick.DatabaseConfigProvider

import slick.driver.JdbcProfile

import scala.concurrent.Future



case class Event(id: Long, name: String, description: String, creator: Long) {

	def patch(name: Option[String], description: Option[String], creator: Option[Long]): Event =
		this.copy(name = name.getOrElse(this.name),
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



	def partialUpdate(id: Long, color: Option[String], description: Option[String], creator: Option[Long]): Future[Int] = {
		import scala.concurrent.ExecutionContext.Implicits.global

		val query = Events.filter(_.id === id)

		val update = query.result.head.flatMap {event =>
			query.update(event.patch(color, description, creator))
		}

		db.run(update)
	}

	def all(): DBIO[Seq[Event]] =
		Events.result

	def insert(event: Event): DBIO[Long] =
		Events returning Events.map(_.id) += event


	private[models] class EventsTable(tag: Tag) extends Table[Event](tag, "events") {

		def id = column[Long]("id", O.AutoInc, O.PrimaryKey)
		def name = column[String]("name")
		def description = column[String]("description")
		def creator = column[Long]("creator")

		def * = (id, name, description, creator) <> (Event.tupled, Event.unapply)
		def ? = (id.?, name.?, description.?, creator.?).shaped.<>({ r => import r._; _1.map(_ => Event.tupled((_1.get, _2.get, _3.get, _4.get))) }, (_: Any) => throw new Exception("Inserting into ? projection not supported."))
	}


}