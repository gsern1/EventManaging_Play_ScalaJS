package controllers

import java.text.{DateFormat, SimpleDateFormat}
import java.util.Date

import models.{EventRepo, User, UserRepo}
import play.api.data.Form
import play.api.mvc._
import play.mvc.Controller.{request, _}
import shared.SharedMessages
import play.api.mvc.Results._
import javax.inject.Inject
import javax.swing.text.DateFormatter

import play.api.Play
import slick.driver.JdbcProfile

import scala.concurrent.{Await, Future}
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
		Redirect(routes.Application.login()).withNewSession
	}

	def profile = Action { request =>
		if (!secured.isLoggedIn(request)) {
			Redirect(routes.Application.login())
		} else {
			Ok(views.html.profile(secured.isLoggedIn(request), Await.result(userRepo.findByName(secured.getUsername(request)), Duration(10, "seconds")).orNull))
		}
	}

	def createEvent = Action { request =>

		val name = request.body.asFormUrlEncoded.get("name").head
		val date = request.body.asFormUrlEncoded.get("date").head
		val description = request.body.asFormUrlEncoded.get("description").head
		val creatorName = request.session.get("username").orNull

		//Parse Date
		//var formatter: DateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
		//val date = formatter.parse(dateString)

		//var dt = new java.util.Date()
		//val sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

		//val creator = Await.result(userRepo.findByName(creatorName), Duration(10, "seconds")).head.id;

		val creator = Await.result(userRepo.findByName(creatorName), Duration(10, "seconds")).head.id;


		if(Await.result(eventRepo.createEvent(name,date,description,creator), Duration(10, "seconds")) != null)
			Redirect(routes.Application.dashboard())
		else
			Redirect(routes.Application.dashboard())

	}
}

