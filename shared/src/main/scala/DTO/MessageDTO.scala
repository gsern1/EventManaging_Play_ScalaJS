package DTO

import java.sql.Timestamp
import java.text.SimpleDateFormat

import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Created by antoi on 08.06.2017.
  */
case class MessageDTO (value: String, date: Timestamp, creator: String){


}

object MessageDTO{
	implicit  val messageWrites = new Writes[MessageDTO]{
		def writes(messageDTO: MessageDTO)= Json.obj(
			"value" -> messageDTO.value,
			"date" -> new SimpleDateFormat("MM/dd/yyyy HH:mm:ss").format(messageDTO.date),
			"creator" -> messageDTO.creator

		)
	}
}
