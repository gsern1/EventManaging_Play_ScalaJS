package controllers

import models.{EventRepo, User, UserRepo}
import play.api.data.Form
import play.api.mvc._
import play.mvc.Controller.{request, _}
import shared.SharedMessages
import play.api.mvc.Results._
import javax.inject.Inject

import play.api.Play
import slick.driver.JdbcProfile

import scala.concurrent.Await
import scala.concurrent.duration.Duration


class Application @Inject()(userRepo: UserRepo, eventRepo: EventRepo, secured: Secured) extends Controller {

	def index = Action { request =>
		Ok(views.html.index(SharedMessages.itWorks, secured.isLoggedIn(request), Await.result(userRepo.findByName(secured.getUsername(request)), Duration(10, "seconds")).orNull))
	}

	def dashboard = Action { request =>
		if (!secured.isLoggedIn(request))
			Redirect(routes.Application.login())
		else
			Ok(views.html.dashboard(secured.isLoggedIn(request), Await.result(userRepo.findByName(secured.getUsername(request)), Duration(10, "seconds")).orNull))
	}

	def register = Action { request =>
		if (secured.isLoggedIn(request))
			Redirect(routes.Application.dashboard())
		else
			Ok(views.html.register(secured.isLoggedIn(request), Await.result(userRepo.findByName(secured.getUsername(request)), Duration(10, "seconds")).orNull))
	}

	def registerUser = Action { request =>
		val username = request.body.asFormUrlEncoded.get("username").head
		val password = request.body.asFormUrlEncoded.get("password").head

		if(Await.result(userRepo.registerUser(username, password), Duration(10, "seconds")) != null)
			Redirect(routes.Application.dashboard()).withSession("username" -> username)
		else
			Redirect(routes.Application.register()).withNewSession.flashing("Register Failed" -> "Duplicate username or password too short.")
	}

	def login = Action { request =>
		if (secured.isLoggedIn(request))
			Redirect(routes.Application.dashboard())
		else
			Ok(views.html.login(secured.isLoggedIn(request), Await.result(userRepo.findByName(secured.getUsername(request)), Duration(10, "seconds")).orNull))
	}

	def loginUser = Action { request =>
		val username = request.body.asFormUrlEncoded.get("username").head
		val password = request.body.asFormUrlEncoded.get("password").head
		if(Await.result(userRepo.authenticate(username, password), Duration(10, "seconds")))
			Redirect(routes.Application.dashboard()).withSession("username" -> username)
		else
			Redirect(routes.Application.login()).withNewSession.flashing("Login Failed" -> "Invalid username or password.")
	}

	def logout = Action { request =>
		Redirect(routes.Application.login()).withSession("username" -> "")
	}

	def profile = Action { request =>
		if (!secured.isLoggedIn(request)) {
			Redirect(routes.Application.login())
		} else {
			Ok(views.html.profile(secured.isLoggedIn(request), Await.result(userRepo.findByName(secured.getUsername(request)), Duration(10, "seconds")).orNull))
		}
	}
}

