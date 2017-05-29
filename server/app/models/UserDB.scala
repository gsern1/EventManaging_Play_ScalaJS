package models

import java.util.{HashMap, Map};

/**
 * Provides an in-memory repository for UserInfo.
 * Storing credentials in the clear is kind of bogus.
 * @author Antoine Drabble
 */
object UserDB {

    var users : Map[String, User] = new HashMap[String, User]()

    /**
     * Adds the specified user to the UserInfoDB.
     * @param username Their username.
     * @param password Their password.
     */
    def addUser(username: String, password: String) {
        users.put(username, new User(username, password));
    }

    /**
     * Returns true if the email represents a known user.
     * @param email The email.
     * @return True if known user.
     */
    def isUser(username: String) : Boolean = {
        users.containsKey(username)
    }

    /**
     * Returns the UserInfo associated with the email, or null if not found.
     * @param username The username.
     * @return The UserInfo.
     */
    def  getUser(username: String) : User = {
        users.get(if(username == null) "" else username)
    }

    /**
     * Returns true if email and password are valid credentials.
     * @param username The username.
     * @param password The password.
     * @return True if email is a valid user email and password is valid for that email.
     */
    def  isValid(username: String, password: String) {
        ((username != null)
                &&
                (password != null)
                &&
                isUser(username)
                &&
                getUser(username).password.equals(password))
    }
}
