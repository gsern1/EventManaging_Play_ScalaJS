package controllers

import java.sql.Timestamp
import java.text.{DateFormat, SimpleDateFormat}
import java.util.Date

import models._
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


class Application @Inject()(userRepo: UserRepo, eventRepo: EventRepo, pictureRepo: PictureRepo, secured: Secured) extends Controller {

	def index = Action { request =>
		Ok(views.html.index(secured.isLoggedIn(request), Await.result(userRepo.findByName(secured.getUsername(request)), Duration(10, "seconds")).orNull))
	}

	def notFound = Action { request =>
		Ok(views.html.notFound(secured.isLoggedIn(request), Await.result(userRepo.findByName(secured.getUsername(request)), Duration(10, "seconds")).orNull))
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

	def event(id: Long) = Action { request =>
		if (!secured.isLoggedIn(request)) {
			Redirect(routes.Application.login())
		} else {
			val event = Event(0, "Paleo festival", Timestamp.valueOf("2018-12-20 08:30:45"),"Chemin du levant 71, 1003 Lausanne", "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Morbi eget turpis tincidunt, cursus dolor quis, luctus ipsum. Cras vulputate pellentesque lacus. Nullam pellentesque luctus felis id ultrices. In pharetra est a ipsum cursus, quis luctus tortor ultricies. Curabitur consectetur gravida tellus, quis tincidunt risus. Nunc egestas imperdiet magna in laoreet. Etiam condimentum iaculis euismod. Suspendisse iaculis, turpis non molestie lobortis, quam nibh semper metus, vel luctus magna massa eget urna. Nullam faucibus euismod diam id cursus. In sed risus nec arcu scelerisque posuere. Donec vel elit sed odio cursus suscipit. Donec pretium arcu eu placerat scelerisque. Ut sit amet urna tempus, congue quam pharetra, cursus purus.\n\nDonec condimentum pellentesque justo, non eleifend sem tincidunt quis. Proin justo nunc, blandit quis congue et, venenatis ac magna. Sed quis maximus ipsum, non blandit lectus. Ut non erat non velit vestibulum sagittis sed at urna. Etiam tellus eros, tristique quis tortor ut, rutrum viverra dolor. Praesent bibendum nec tellus sed consequat. Cras ac metus dui. Mauris placerat sem nec tempus vulputate. Donec sit amet molestie diam, ut luctus libero.\n\nNulla consectetur felis id ultrices ultricies. Fusce ornare tempor orci quis gravida. Praesent mollis, enim eget varius rhoncus, diam mi tristique sem, sit amet congue arcu risus ac nisi. Nulla iaculis, risus non posuere elementum, eros quam sagittis purus, fermentum gravida neque nulla sed urna. Suspendisse accumsan aliquet lorem ac ultrices. Maecenas maximus eleifend rhoncus. Ut ultricies, neque sit amet sodales imperdiet, lorem nulla lobortis tortor, sed imperdiet enim purus et justo. Integer sed posuere odio, id feugiat metus. Nam lectus mauris, auctor ac facilisis eget, porta nec orci. Quisque in justo varius, vestibulum velit eget, ultrices ante. Ut consectetur tincidunt varius. Praesent bibendum nisi id mauris vulputate, nec pulvinar ante tincidunt. Quisque venenatis, lorem a eleifend faucibus, dolor ipsum tempus leo, in dapibus magna nunc quis ex.",2, 1)

			Ok(views.html.event(secured.isLoggedIn(request), Await.result(userRepo.findByName(secured.getUsername(request)), Duration(10, "seconds")).orNull, event))
		}
	}

	def createEvent = Action (parse.multipartFormData){ request =>

		request.body.file("picture").map { picture =>
			import java.io.File
			val filename = picture.filename
			val contentType = picture.contentType
			picture.ref.moveTo(new File(s"/public/images/events/$filename"))



			val name = request.body.dataParts.getOrElse("name","").toString
			val dateString = request.body.dataParts.getOrElse("date","").toString
			val location = request.body.dataParts.getOrElse("location","").toString
			val description = request.body.dataParts.getOrElse("description","").toString
			val creatorName = request.session.get("username").orNull



			val date : Timestamp= Timestamp.valueOf(dateString.toString)
			val creator = Await.result(userRepo.findByName(creatorName), Duration(10, "seconds")).head.id


			if(Await.result(eventRepo.createEvent(name,date,location,description,creator,0), Duration(10, "seconds")) != null &&
					Await.result(pictureRepo.createPicture(filename), Duration(10, "seconds")) != null)
				Redirect(routes.Application.dashboard())
			else
				Redirect(routes.Application.dashboard())
		}.getOrElse {
			Redirect(routes.Application.dashboard()).flashing(
				"error" -> "Missing file")
		}



	}

	def editEvent(id: Long) = Action { request =>
		if (!secured.isLoggedIn(request)) {
			Redirect(routes.Application.login())
		} else {
			val event = Event(0, "Paleo festival", Timestamp.valueOf("2018-12-20 08:30:45"),"L'asse", "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Morbi eget turpis tincidunt, cursus dolor quis, luctus ipsum. Cras vulputate pellentesque lacus. Nullam pellentesque luctus felis id ultrices. In pharetra est a ipsum cursus, quis luctus tortor ultricies. Curabitur consectetur gravida tellus, quis tincidunt risus. Nunc egestas imperdiet magna in laoreet. Etiam condimentum iaculis euismod. Suspendisse iaculis, turpis non molestie lobortis, quam nibh semper metus, vel luctus magna massa eget urna. Nullam faucibus euismod diam id cursus. In sed risus nec arcu scelerisque posuere. Donec vel elit sed odio cursus suscipit. Donec pretium arcu eu placerat scelerisque. Ut sit amet urna tempus, congue quam pharetra, cursus purus.\n\nDonec condimentum pellentesque justo, non eleifend sem tincidunt quis. Proin justo nunc, blandit quis congue et, venenatis ac magna. Sed quis maximus ipsum, non blandit lectus. Ut non erat non velit vestibulum sagittis sed at urna. Etiam tellus eros, tristique quis tortor ut, rutrum viverra dolor. Praesent bibendum nec tellus sed consequat. Cras ac metus dui. Mauris placerat sem nec tempus vulputate. Donec sit amet molestie diam, ut luctus libero.\n\nNulla consectetur felis id ultrices ultricies. Fusce ornare tempor orci quis gravida. Praesent mollis, enim eget varius rhoncus, diam mi tristique sem, sit amet congue arcu risus ac nisi. Nulla iaculis, risus non posuere elementum, eros quam sagittis purus, fermentum gravida neque nulla sed urna. Suspendisse accumsan aliquet lorem ac ultrices. Maecenas maximus eleifend rhoncus. Ut ultricies, neque sit amet sodales imperdiet, lorem nulla lobortis tortor, sed imperdiet enim purus et justo. Integer sed posuere odio, id feugiat metus. Nam lectus mauris, auctor ac facilisis eget, porta nec orci. Quisque in justo varius, vestibulum velit eget, ultrices ante. Ut consectetur tincidunt varius. Praesent bibendum nisi id mauris vulputate, nec pulvinar ante tincidunt. Quisque venenatis, lorem a eleifend faucibus, dolor ipsum tempus leo, in dapibus magna nunc quis ex.", 1, 1)
			Ok(views.html.editEvent(secured.isLoggedIn(request), Await.result(userRepo.findByName(secured.getUsername(request)), Duration(10, "seconds")).orNull, event))
		}
	}

}

