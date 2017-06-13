package models

/**
  * Created by guillaume on 06.06.17.
  */


import javax.inject.Inject

import play.api.db.slick.DatabaseConfigProvider
import slick.driver.JdbcProfile

import scala.concurrent.Future


/**
  * Model for pictures
  *
  * @param id  the picture's id
  * @param url the picture's url
  */
case class Picture(id: Long, url: String)

/**
  * Repository for Pictures, used as a DAO
  *
  * @param dbConfigProvider
  */
class PictureRepo @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) {

	val dbConfig = dbConfigProvider.get[JdbcProfile]
	val db = dbConfig.db

	import dbConfig.driver.api._

	private[models] val Pictures = TableQuery[PicturesTable]


	def findById(id: Long): Future[Picture] =
		db.run(Pictures.filter(_.id === id).result.head)

	def createPicture(url: String): Future[Long] = {

		val picture = Picture(0, url)
		db.run(Pictures returning Pictures.map(_.id) += picture)

	}

	/**
	  * Definition of the pictures table properties
	  *
	  * @param tag
	  */
	private[models] class PicturesTable(tag: Tag) extends Table[Picture](tag, "pictures") {

		def id = column[Long]("id", O.AutoInc, O.PrimaryKey)

		def url = column[String]("url")


		def * = (id, url) <> (Picture.tupled, Picture.unapply)

		def ? = (id.?, url.?).shaped.<>({ r => import r._; _1.map(_ => Picture.tupled((_1.get, _2.get))) }, (_: Any) => throw new Exception("Inserting into ? projection not supported."))
	}


}
