import play.api.http.HttpErrorHandler
import play.api.mvc._
import play.api.mvc.Results._

import scala.concurrent._
import javax.inject.{Inject, Singleton}

import controllers.Secured
import models.{EventRepo, MessageRepo, PictureRepo, UserRepo}
import play.mvc.Http

import scala.concurrent.duration.Duration;

@Singleton
class ErrorHandler @Inject()(userRepo: UserRepo, eventRepo: EventRepo, pictureRepo: PictureRepo, messageRepo: MessageRepo, secured: Secured) extends HttpErrorHandler {

  def onClientError(request: RequestHeader, statusCode: Int, message: String) = {
    statusCode match {
      case Http.Status.NOT_FOUND =>
        Future.successful(
          NotFound(views.html.notFound(secured.isLoggedIn(request), Await.result(userRepo.findByName(secured.getUsername(request)), Duration(10, "seconds")).orNull))
        )
      case _ =>
        Future.successful(
          InternalServerError("A client error occurred: " + message)
        )
    }
  }

  def onServerError(request: RequestHeader, exception: Throwable) = {
    exception.printStackTrace()
    Future.successful(
      InternalServerError("A server error occurred: " + exception.getMessage)
    )
  }
}