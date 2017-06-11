package controllers

import javax.inject.Inject

import DTO.MessageDTO
import models.{Event, EventRepo, MessageRepo, UserRepo}
import play.api.libs.json.Json
import play.api.mvc._
import shared.SharedMessages

import scala.concurrent.Await
import scala.concurrent.duration.Duration


class API @Inject()(userRepo: UserRepo, eventRepo: EventRepo, secured: Secured, messageRepo: MessageRepo) extends Controller {
	def sendMessage(id: Long) = Action { request =>
		if (!secured.isLoggedIn(request))
			Unauthorized
		else{
			val value = request.body.asText.get
			val creatorName = request.session.get("username").orNull
			val creator = Await.result(userRepo.findByName(creatorName), Duration(10, "seconds")).head.id
			val messageId = Await.result(messageRepo.createMessage(value,creator,id), Duration(10, "seconds"))
			val date = Await.result(messageRepo.findById(messageId), Duration(10, "seconds")).date
			Created(Json.toJson(MessageDTO(value,date,creatorName)))
		}
	}
}

