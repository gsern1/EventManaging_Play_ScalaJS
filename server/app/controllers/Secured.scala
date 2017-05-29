package controllers

import models.{User, UserDB}
import play.api.mvc.{Action, Request, Result}
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
object Secured {

    /**
      * Get the username in session.
      * @param ctx The context.
      * @return The username.
      */
    def getUsername(request: Request[Any]) = request.session.get("username").orNull

    /**
     * Instruct authenticator to automatically redirect to login page if unauthorized.
     * @param ctx The context.
     * @return The login page.
     */
    /*def onUnauthorized(ctx: Context) : Result = {
        Redirect(routes.Application.login())
    }*/

    /**
     * True if there is a logged in user, false otherwise.
     * @param ctx The context.
     * @return True if user is logged in.
     */
    def isLoggedIn(request: Request[Any]) : Boolean = {
        getUsername(request) != null && UserDB.isUser(getUsername(request))
    }

    /**
     * Return the UserInfo of the logged in user, or null if no user is logged in.
     * @param ctx The context.
     * @return The UserInfo, or null.
     */
    def getUser(request: Request[Any]) : User = {
        UserDB.getUser(getUsername(request))
    }
}