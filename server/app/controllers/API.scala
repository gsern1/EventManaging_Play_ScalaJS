package controllers

import javax.inject.Inject

import DTO.MessageDTO
import models._
import play.api.libs.json.Json
import play.api.mvc._

import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
  * Controller to implement the dynamic actions on the front-end side
  *
  * @param userRepo
  * @param eventRepo
  * @param secured
  * @param messageRepo
  * @param eventParticipantRepo
  */
class API @Inject()(userRepo: UserRepo, eventRepo: EventRepo, secured: Secured, messageRepo: MessageRepo, eventParticipantRepo: EventParticipantRepo) extends Controller {
	/**
	  * Send a message to an event
	  *
	  * @param id the event's id
	  * @return The http response
	  */
	def sendMessage(id: Long) = Action { request =>
		if (!secured.isLoggedIn(request))
			Unauthorized
		else {
			val value = request.body.asText.get
			val creatorName = request.session.get("username").orNull
			val creator = Await.result(userRepo.findByName(creatorName), Duration(10, "seconds")).head.id
			//Save to message using slick
			val messageId = Await.result(messageRepo.createMessage(value, creator, id), Duration(10, "seconds"))
			val date = Await.result(messageRepo.findById(messageId), Duration(10, "seconds")).date
			Created(Json.toJson(MessageDTO(value, date, creatorName)))
		}
	}

	/**
	  * Notify participation OR cancellation of participation to an event
	  *
	  * @param id the event's id
	  * @return The http response
	  */
	def participate(id: Long) = Action { request =>
		if (!secured.isLoggedIn(request))
			Unauthorized
		else {
			val username = request.session.get("username").orNull
			val user = Await.result(userRepo.findByName(username), Duration(10, "seconds")).orNull.id
			//The user is already participating -> cancel participation
			if (!Await.result(eventParticipantRepo.findByEventIdAndUserId(id, user), Duration(10, "seconds")).isEmpty)
				if (Await.result(eventParticipantRepo.deleteParticipation(id, user), Duration(10, "seconds")) > 0)
					NoContent
				else
					NotFound
			else {
				//The user is not participating yet -> notify participation
				if (Await.result(eventParticipantRepo.createParticipation(id, user), Duration(10, "seconds")) != null)
					Created
				else
					NotFound

			}
		}
	}


}

