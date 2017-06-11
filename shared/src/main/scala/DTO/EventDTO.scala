package DTO

import java.sql.Timestamp

/**
  * Created by antoi on 08.06.2017.
  */
case class EventDTO(id: Long, name: String, date: Timestamp, location:String, description: String, creator: Long, picture : Option[PictureDTO], participant: Boolean)