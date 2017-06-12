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
import play.api.Play
import slick.driver.JdbcProfile

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration


class Application @Inject()(userRepo: UserRepo, eventRepo: EventRepo, pictureRepo: PictureRepo, messageRepo: MessageRepo, eventParticipantRepo: EventParticipantRepo, secured: Secured) extends Controller {

	def index = Action { request =>
		Ok(views.html.index(secured.isLoggedIn(request), Await.result(userRepo.findByName(secured.getUsername(request)), Duration(10, "seconds")).orNull))
	}

	def notFound = Action { request =>
		Ok(views.html.notFound(secured.isLoggedIn(request), Await.result(userRepo.findByName(secured.getUsername(request)), Duration(10, "seconds")).orNull))
	}

	def dashboard = Action { request =>
		if (!secured.isLoggedIn(request))
			Redirect(routes.Application.login())
		else {
			val user : User = Await.result(userRepo.findByName(secured.getUsername(request)), Duration(10, "seconds")).orNull
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

	def register = Action { request =>
		if (secured.isLoggedIn(request))
			Redirect(routes.Application.dashboard())
		else
			Ok(views.html.register(secured.isLoggedIn(request), Await.result(userRepo.findByName(secured.getUsername(request)), Duration(10, "seconds")).orNull))
	}

	def registerUser = Action { request =>
		val username = request.body.asFormUrlEncoded.get("username").head
		val password = request.body.asFormUrlEncoded.get("password").head

		if (Await.result(userRepo.registerUser(username, password), Duration(10, "seconds")) != null)
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
		if (Await.result(userRepo.authenticate(username, password), Duration(10, "seconds")))
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
			val user = Await.result(userRepo.findByName(secured.getUsername(request)), Duration(10, "seconds")).orNull
			val events = Await.result(eventRepo.findByUserId(user.id), Duration(10, "seconds")).sortBy(_.date.getTime).reverse.map(e => EventDTO(e.id, e.name, e.date, e.location, e.description, e.creator, if (e.picture.isDefined) Option(PictureDTO((Await.result(pictureRepo.findById(e.picture.get), Duration(10, "seconds"))).url)) else Option.empty, true))
			Ok(views.html.profile(secured.isLoggedIn(request), user, events))
		}
	}

	def event(id: Long) = Action { request =>
		if (!secured.isLoggedIn(request)) {
			Redirect(routes.Application.login())
		} else {
			val user : User = Await.result(userRepo.findByName(secured.getUsername(request)), Duration(10, "seconds")).orNull
			val event = Await.result(eventRepo.findById(id), Duration(10, "seconds"))
			val messages = Await.result(messageRepo.findByEvent(event.id), Duration(10, "seconds"))
			val creator = Await.result(userRepo.findById(event.creator), Duration(10, "seconds"))
			val formatter = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss")
			if (event.picture.isDefined) {
				val picture = Await.result(pictureRepo.findById(event.picture.get), Duration(10, "seconds"))
				Ok(views.html.event(secured.isLoggedIn(request), Await.result(userRepo.findByName(secured.getUsername(request)), Duration(10, "seconds")).orNull, EventDTO(event.id, event.name, event.date, event.location, event.description, event.creator, Option(PictureDTO(picture.url)), !Await.result(eventParticipantRepo.findByEventIdAndUserId(event.id, user.id), Duration(10, "seconds")).isEmpty), creator.orNull, messages.map(m => MessageDTO(m.value, m.date, Await.result(userRepo.findById(m.creator), Duration(10, "seconds")).orNull.username)), formatter))
			} else {
				Ok(views.html.event(secured.isLoggedIn(request), Await.result(userRepo.findByName(secured.getUsername(request)), Duration(10, "seconds")).orNull, EventDTO(event.id, event.name, event.date, event.location, event.description, event.creator, Option.empty, !Await.result(eventParticipantRepo.findByEventIdAndUserId(event.id, user.id), Duration(10, "seconds")).isEmpty), creator.orNull, messages.map(m => MessageDTO(m.value, m.date, Await.result(userRepo.findById(m.creator), Duration(10, "seconds")).orNull.username)), formatter))
			}
		}
	}

	def createEvent = Action(parse.multipartFormData) { request =>
		import java.io.File
		val picture = request.body.file("picture").get

		// TODO Generate unique file name with uuid as below
		//val filename = java.util.UUID.randomUUID.toString + "." + picture.filename.split(".").last


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
			Await.result(eventParticipantRepo.createParticipation(eventId,creator), Duration(10, "seconds"))
			Redirect(routes.Application.event(eventId))
		} else {
			val eventId = Await.result(eventRepo.createEvent(name, date, location, description, creator, Option.empty), Duration(10, "seconds"))
			Redirect(routes.Application.event(eventId))
		}
	}

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

	def editEventPost(id: Long) = Action(parse.multipartFormData) { request =>
		import java.io.File
		val picture = request.body.file("picture").get

		// TODO Generate unique file name with uuid as below
		//val filename = java.util.UUID.randomUUID.toString + "." + picture.filename.split(".").last

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
			Await.result(eventRepo.updateEvent(id, name, date, location, description, creator, Option(pictureId)), Duration(10, "seconds"))
			Redirect(routes.Application.event(id))
		} else {
			Await.result(eventRepo.updateEvent(id, name, date, location, description, creator, Option.empty), Duration(10, "seconds"))
			Redirect(routes.Application.event(id))
		}
	}

  def updateProfile() = play.mvc.Results.TODO
}

