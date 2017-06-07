package models

/**
  * Created by guillaume on 06.06.17.
  */


import javax.inject.Inject
import play.api.db.slick.DatabaseConfigProvider
import slick.driver.JdbcProfile
import java.sql.Timestamp

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}



case class Picture(id: Long, url: String)

class PictureRepo @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) {

	val dbConfig = dbConfigProvider.get[JdbcProfile]
	val db = dbConfig.db
	import dbConfig.driver.api._

	private[models] val Pictures = TableQuery[PicturesTable]


	def findById(id: Long): Future[Picture] =
		db.run(Pictures.filter(_.id === id).result.head)



	def createPicture(url: String): Future[Long] = {

		val picture = Picture(0,url)
		db.run(Pictures returning Pictures.map(_.id) += picture)

	}





	private[models] class PicturesTable(tag: Tag) extends Table[Picture](tag, "pictures") {

		//implicit val dateColumnType = DateMapper.utilDate2SqlDate

		def id = column[Long]("id", O.AutoInc, O.PrimaryKey)
		def url = column[String]("url")


		def * = (id, url) <> (Picture.tupled, Picture.unapply)
		def ? = (id.?, url.?).shaped.<>({ r => import r._; _1.map(_ => Picture.tupled((_1.get, _2.get))) }, (_: Any) => throw new Exception("Inserting into ? projection not supported."))
	}


}
