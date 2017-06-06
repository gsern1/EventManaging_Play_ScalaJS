package controllers

import javax.inject.Inject

import models.{Event, EventRepo, UserRepo}
import play.api.mvc._
import shared.SharedMessages

import scala.concurrent.Await
import scala.concurrent.duration.Duration


class API @Inject()(userRepo: UserRepo, eventRepo: EventRepo, secured: Secured) extends Controller {

	def sendMessage(id: Long) = Action { request =>
		val message = request.body.asFormUrlEncoded.get("message").head
		if (!secured.isLoggedIn(request))
			Unauthorized
		else{
			Created
		}
	}
}

