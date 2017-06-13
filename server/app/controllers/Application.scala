package controllers

import java.sql.Timestamp
import java.text.{DateFormat, SimpleDateFormat}
import java.util.Date

import models._
import play.api.data.Form
import play.api.mvc._
import play.mvc.Controller.{request, _}
import play.api.mvc.Results._
import javax.inject.Inject
import javax.swing.text.DateFormatter

import DTO.{EventDTO, MessageDTO, PictureDTO}
import play.Configuration
import play.api.Play
import slick.driver.JdbcProfile

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration

/**
  * Regroups all the controllers of the application
  *
  * @param configuration
  * @param userRepo
  * @param eventRepo
  * @param pictureRepo
  * @param messageRepo
  * @param eventParticipantRepo
  * @param secured
  */
class Application @Inject()(configuration: Configuration, userRepo: UserRepo, eventRepo: EventRepo, pictureRepo: PictureRepo, messageRepo: MessageRepo, eventParticipantRepo: EventParticipantRepo, secured: Secured) extends Controller {
	/**
	  * Fetch index page
	  *
	  * @return an HTTP response containing the index page
	  */
	def index = Action { request =>
		Ok(views.html.index(secured.isLoggedIn(request), Await.result(userRepo.findByName(secured.getUsername(request)), Duration(10, "seconds")).orNull))
	}

	/**
	  * Handles 404 notFoud errors
	  *
	  * @return an HTTP response containing the error page
	  */
	def notFound = Action { request =>
		Ok(views.html.notFound(secured.isLoggedIn(request), Await.result(userRepo.findByName(secured.getUsername(request)), Duration(10, "seconds")).orNull))
	}

	/**
	  * Fetch dashboard page
	  *
	  * @return an HTTP response containing the index page
	  */
	def dashboard = Action { request =>
		if (!secured.isLoggedIn(request))
			Redirect(routes.Application.login())
		else {
			val user: User = Await.result(userRepo.findByName(secured.getUsername(request)), Duration(10, "seconds")).orNull
			val events = Await.result(eventRepo.findAll(), Duration(10, "seconds"))
			val pastEvents = events.filter(e => e.date.getTime <= System.currentTimeMillis()).sortBy(_.date.getTime).map(e => EventDTO(e.id, e.name, e.date, e.location, e.description, e.creator, if (e.picture.isDefined) Option(PictureDTO(Await.result(pictureRepo.findById(e.picture.get), Duration(10, "seconds")).url)) else Option.empty, !Await.result(eventParticipantRepo.findByEventIdAndUserId(e.id, user.id), Duration(10, "seconds")).isEmpty))
			val comingEvents = events
					.filter(e => e.date.getTime >= System.currentTimeMillis())
					.sortBy(_.date.getTime)
					.map(e => EventDTO(e.id,
						e.name,
						e.date,
						e.location,
						e.description,
						e.creator,
						if (e.picture.isDefined) Option(PictureDTO(Await.result(pictureRepo.findById(e.picture.get), Duration(10, "seconds")).url))
						else Option.empty,
						!Await.result(eventParticipantRepo.findByEventIdAndUserId(e.id, user.id), Duration(10, "seconds")).isEmpty))
			Ok(views.html.dashboard(secured.isLoggedIn(request), Await.result(userRepo.findByName(secured.getUsername(request)), Duration(10, "seconds")).orNull, comingEvents, pastEvents))
		}
	}

	/**
	  * Fetch register page
	  *
	  * @return an HTTP response containing the register page
	  */
	def register = Action { request =>
		if (secured.isLoggedIn(request))
			Redirect(routes.Application.dashboard())
		else
			Ok(views.html.register(secured.isLoggedIn(request), Await.result(userRepo.findByName(secured.getUsername(request)), Duration(10, "seconds")).orNull))
	}

	/**
	  *
	  * Register an user to the application, redirects to dashboard in case of success
	  *
	  * @return an HTTP response to redirect the user to the dashboard in case of succes, or the error message otherwise
	  */
	def registerUser = Action { request =>
		val username = request.body.asFormUrlEncoded.get("username").head
		val password = request.body.asFormUrlEncoded.get("password").head
		val serverPassword = request.body.asFormUrlEncoded.get("server_password").head

		if (Await.result(userRepo.registerUser(username, password), Duration(10, "seconds")) != null && serverPassword == configuration.getString("server.password"))
			Redirect(routes.Application.dashboard()).withSession("username" -> username)
		else
			Ok(views.html.register(secured.isLoggedIn(request), Await.result(userRepo.findByName(secured.getUsername(request)), Duration(10, "seconds")).orNull, "Duplicate username or password too short or wrong server password."))
	}

	/**
	  * Fetch login page
	  *
	  * @return an HTTP response containing the login page
	  */
	def login = Action { request =>
		if (secured.isLoggedIn(request))
			Redirect(routes.Application.dashboard())
		else
			Ok(views.html.login(secured.isLoggedIn(request), Await.result(userRepo.findByName(secured.getUsername(request)), Duration(10, "seconds")).orNull))
	}

	/**
	  * Login an User
	  *
	  * @return an HTTP response to redirect the user to the dashboard in case of succes, or to the login page otherwise
	  */
	def loginUser = Action { request =>
		val username = request.body.asFormUrlEncoded.get("username").head
		val password = request.body.asFormUrlEncoded.get("password").head
		if (Await.result(userRepo.authenticate(username, password), Duration(10, "seconds")))
			Redirect(routes.Application.dashboard()).withSession("username" -> username)
		else
			Redirect(routes.Application.login()).withNewSession.flashing("Login Failed" -> "Invalid username or password.")
	}

	/**
	  * Log an user out
	  *
	  * @return an HTTP response to redirect the user to the login page with a new session
	  */
	def logout = Action { request =>
		Redirect(routes.Application.login()).withNewSession
	}

	/**
	  * Fetch the page of an event
	  *
	  * @param id the id of the event to fetch
	  * @return an HTTP response containing the event page, with or without picture
	  */
	def event(id: Long) = Action { request =>
		if (!secured.isLoggedIn(request)) {
			Redirect(routes.Application.login())
		} else {
			val user: User = Await.result(userRepo.findByName(secured.getUsername(request)), Duration(10, "seconds")).orNull
			val event = Await.result(eventRepo.findById(id), Duration(10, "seconds"))
			val messages = Await.result(messageRepo.findByEvent(event.id), Duration(10, "seconds"))
			val creator = Await.result(userRepo.findById(event.creator), Duration(10, "seconds"))
			val formatter = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss")
			val participants = Await.result(eventParticipantRepo.findByEventId(event.id), Duration(10, "seconds")).map(ep => Await.result(userRepo.findById(ep.userID), Duration(10, "seconds")).get.username)
			if (event.picture.isDefined) {
				val picture = Await.result(pictureRepo.findById(event.picture.get), Duration(10, "seconds"))
				Ok(views.html.event(secured.isLoggedIn(request), Await.result(userRepo.findByName(secured.getUsername(request)), Duration(10, "seconds")).orNull, EventDTO(event.id, event.name, event.date, event.location, event.description, event.creator, Option(PictureDTO(picture.url)), !Await.result(eventParticipantRepo.findByEventIdAndUserId(event.id, user.id), Duration(10, "seconds")).isEmpty), creator.orNull, messages.map(m => MessageDTO(m.value, m.date, Await.result(userRepo.findById(m.creator), Duration(10, "seconds")).orNull.username)), formatter, participants, user.id == event.creator))
			} else {
				Ok(views.html.event(secured.isLoggedIn(request), Await.result(userRepo.findByName(secured.getUsername(request)), Duration(10, "seconds")).orNull, EventDTO(event.id, event.name, event.date, event.location, event.description, event.creator, Option.empty, !Await.result(eventParticipantRepo.findByEventIdAndUserId(event.id, user.id), Duration(10, "seconds")).isEmpty), creator.orNull, messages.map(m => MessageDTO(m.value, m.date, Await.result(userRepo.findById(m.creator), Duration(10, "seconds")).orNull.username)), formatter, participants, user.id == event.creator))
			}
		}
	}

	/**
	  * Create a new event
	  *
	  * @return an HTTP response redirecting to the newly created event's page, with or without picture
	  */
	def createEvent = Action(parse.multipartFormData) { request =>
		import java.io.File
		val picture = request.body.file("picture").get

		val name = request.body.dataParts("name").head
		val dateString = request.body.dataParts("date").head
		val location = request.body.dataParts("location").head
		val description = request.body.dataParts("description").head
		val creatorName = request.session.get("username").orNull
		val date: Timestamp = Timestamp.valueOf(dateString)
		val creator = Await.result(userRepo.findByName(creatorName), Duration(10, "seconds")).head.id

		if (!picture.filename.isEmpty) {
			picture.ref.moveTo(new File(System.getProperty("user.dir") + "/server/public/events/" + picture.filename))
			val pictureId = Await.result(pictureRepo.createPicture(picture.filename), Duration(10, "seconds"))
			val eventId = Await.result(eventRepo.createEvent(name, date, location, description, creator, Option(pictureId)), Duration(10, "seconds"))
			Await.result(eventParticipantRepo.createParticipation(eventId, creator), Duration(10, "seconds"))
			Redirect(routes.Application.event(eventId))
		} else {
			val eventId = Await.result(eventRepo.createEvent(name, date, location, description, creator, Option.empty), Duration(10, "seconds"))
			Redirect(routes.Application.event(eventId))
		}
	}

	/**
	  * Fetch the editEvent page
	  *
	  * @param id the id of the event to edit
	  * @return an HTTP response containing the editEvent page, with or without picture
	  */
	def editEvent(id: Long) = Action { request =>
		if (!secured.isLoggedIn(request)) {
			Redirect(routes.Application.login())
		} else {
			val event = Await.result(eventRepo.findById(id), Duration(10, "seconds"))

			if (event.picture.isDefined) {
				val picture = Await.result(pictureRepo.findById(event.picture.get), Duration(10, "seconds"))
				Ok(views.html.editEvent(secured.isLoggedIn(request), Await.result(userRepo.findByName(secured.getUsername(request)), Duration(10, "seconds")).orNull, event, Option(picture)))
			} else {
				Ok(views.html.editEvent(secured.isLoggedIn(request), Await.result(userRepo.findByName(secured.getUsername(request)), Duration(10, "seconds")).orNull, event, Option.empty))
			}
		}
	}

	/**
	  * Update an event, only allowed for the event's creator
	  *
	  * @param id the id of the event to update
	  * @return an HTTP response redirecting the event page, with or without picture
	  */
	def editEventPost(id: Long) = Action(parse.multipartFormData) { request =>
		import java.io.File
		val picture = request.body.file("picture").get

		val name = request.body.dataParts("name").head
		val dateString = request.body.dataParts("date").head
		val location = request.body.dataParts("location").head
		val description = request.body.dataParts("description").head
		val creatorName = request.session.get("username").get
		val date: Timestamp = Timestamp.valueOf(dateString)
		val creator = Await.result(userRepo.findByName(creatorName), Duration(10, "seconds")).head.id

		if (!picture.filename.isEmpty) {
			picture.ref.moveTo(new File(System.getProperty("user.dir") + "/server/public/events/" + picture.filename))
			val pictureId = Await.result(pictureRepo.createPicture(picture.filename), Duration(10, "seconds"))
			Await.result(eventRepo.updateEvent(id, name, date, location, description, creator, Option(pictureId)), Duration(10, "seconds"))
			Redirect(routes.Application.event(id))
		} else {
			Await.result(eventRepo.updateEvent(id, name, date, location, description, creator, Option.empty), Duration(10, "seconds"))
			Redirect(routes.Application.event(id))
		}
	}

	/**
	  * Fetch the profile page
	  *
	  * @return an HTTP response containing the profile page if the user is logged in or a redirection to login otherwise
	  */
	def profile = Action { request =>
		if (!secured.isLoggedIn(request)) {
			Redirect(routes.Application.login())
		} else {
			val user = Await.result(userRepo.findByName(secured.getUsername(request)), Duration(10, "seconds")).orNull
			val events = Await.result(eventRepo.findByUserId(user.id), Duration(10, "seconds")).sortBy(_.date.getTime).reverse.map(e => EventDTO(e.id, e.name, e.date, e.location, e.description, e.creator, if (e.picture.isDefined) Option(PictureDTO((Await.result(pictureRepo.findById(e.picture.get), Duration(10, "seconds"))).url)) else Option.empty, true))
			Ok(views.html.profile(secured.isLoggedIn(request), user, events))
		}
	}

	/**
	  * Update user profile
	  *
	  * @return an HTTP response redirecting the profile page
	  */
	def updateProfile() = Action { request =>
		val username = request.body.asFormUrlEncoded.get("username").head
		val password = request.body.asFormUrlEncoded.get("password").head
		val user = Await.result(userRepo.findByName(request.session.get("username").orNull), Duration(10, "seconds")).get

		Await.result(userRepo.updateUser(user.id, username, password), Duration(10, "seconds"))

		Redirect(routes.Application.profile()).withSession("username" -> username)
	}
}

