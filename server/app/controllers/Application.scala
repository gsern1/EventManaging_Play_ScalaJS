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


class Application @Inject()(userRepo: UserRepo, eventRepo: EventRepo) extends Controller {

	def index = Action { request =>
		Ok(views.html.index(SharedMessages.itWorks, Secured.isLoggedIn(request), Secured.getUser(request)))
	}


	def dashboard = Action { request =>
		if (!Secured.isLoggedIn(request))
			Redirect(routes.Application.login())
		else
			Ok(views.html.dashboard(Secured.isLoggedIn(request), Secured.getUser(request)))
	}


	def register = Action { request =>
		if (Secured.isLoggedIn(request))
			Redirect(routes.Application.dashboard())
		else
			Ok(views.html.register(Secured.isLoggedIn(request), Secured.getUser(request)))
	}



	def registerUser = Action.async { implicit request =>
		val username = request.body.asFormUrlEncoded.get("username").head
		val password = request.body.asFormUrlEncoded.get("password").head
		userRepo.registerUser(username, password).map(id => Ok(id.toString))
	}

	def login = Action { request =>
		if (Secured.isLoggedIn(request))
			Redirect(routes.Application.dashboard())
		else
			Ok(views.html.login(Secured.isLoggedIn(request), Secured.getUser(request)))
	}

	def loginUser = Action.async { implicit request =>

		val username = request.body.asFormUrlEncoded.get("username").head
		val password = request.body.asFormUrlEncoded.get("password").head
		userRepo.authenticate(username, password).map(b =>
					if(b)Redirect(routes.Application.dashboard()).withSession("username" -> username)
					else Redirect(routes.Application.login()).withNewSession.flashing("Login Failed" -> "Invalid username or password.")
		)
	}

	def profile() = Action { request =>
		if (!Secured.isLoggedIn(request))
			Redirect(routes.Application.login())
		else
			Ok(views.html.profile(Secured.isLoggedIn(request), Secured.getUser(request)))
	}



}

