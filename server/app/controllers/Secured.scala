package controllers

import java.time.Duration

import com.google.inject.Inject
import com.sun.corba.se.impl.orbutil.closure.Future
import models.{EventRepo, User, UserRepo}
import play.api.mvc._
import play.mvc.Http.Context
import play.mvc.Controller._

/**
  * Implement authorization for this system.
  * getUserName() and onUnauthorized override superclass methods to restrict
  * access to the profile() page to logged in users.
  *
  * getUser(), isLoggedIn(), and getUserInfo() provide static helper methods so that controllers
  * can know if there is a logged in user.
  *
  * @author Philip Johnson
  */
class Secured @Inject()(userRepo: UserRepo) {

    /**
      * Get the username in session.
      *
      * @param ctx The context.
      * @return The username.
      */
    def getUsername(request: Request[Any]) = request.session.get("username").orNull

    def getUsername(request: RequestHeader) = request.session.get("username").orNull

    /**
      * True if there is a logged in user, false otherwise.
      *
      * @param ctx The context.
      * @return True if user is logged in.
      */
    def isLoggedIn(request: Request[Any]): Boolean = {
        getUsername(request) != null
    }

    def isLoggedIn(request: RequestHeader): Boolean = {
        getUsername(request) != null
    }
}