package example

import DTO.MessageDTO
import org.scalajs.dom
import org.scalajs.dom.ext.Ajax
import org.scalajs.dom.html
import org.scalajs.dom.raw.HTMLFormElement
import play.api.libs.json.Json

import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js
import scala.util.{Failure, Success}
import js.Dynamic.{global => g}

/**
  * Object regrouping the methods used to dynamicyally interact with the front-end using ScalJS
  */
object EventJS extends js.JSApp {
	/**
	  * Add the required event listeners to the corresponding buttons
	  */
	def main(): Unit = {
		if (dom.document.getElementById("send") != null) {
			dom.document.getElementById("send").addEventListener("click", { (e0: dom.Event) =>
				sendMessage()
			}, false)
		}

		if (dom.document.getElementById("location") != null) {
			dom.document.getElementById("location").asInstanceOf[html.Input].addEventListener("input", { (e0: dom.Event) =>
				updateLocation()
			}, false)
		}

		if (dom.document.getElementById("participate") != null) {
			dom.document.getElementById("participate").addEventListener("click", { (e0: dom.Event) =>
				participate()
			}, false)
		}

		if (dom.document.getElementById("dontparticipate") != null) {
			dom.document.getElementById("dontparticipate").addEventListener("click", { (e0: dom.Event) =>
				dontParticipate()
			}, false)
		}
	}

	/**
	  * Send a message within an evemt
	  */
	def sendMessage(): Unit = {
		val url = "/events/" + dom.document.getElementById("eventId").getAttribute("value") + "/messages"
		val f = Ajax.post(url, data = dom.document.getElementById("message").asInstanceOf[html.Input].value)
		f.onComplete {
			case Success(xhr) =>
				dom.document.getElementById("messages").innerHTML = " <div class=\"message\">\n" +
						"<p>" + js.JSON.parse(xhr.responseText).value + "</p>\n" +
						"<i class=\"pull-right\">Created by " + js.JSON.parse(xhr.responseText).creator + "</i>\n" +
						"<i>Sent at " + js.JSON.parse(xhr.responseText).date + "</i>\n" +
						"</div>\n" +
						"<hr/>" +
						dom.document.getElementById("messages").innerHTML
				dom.document.getElementById("message").asInstanceOf[html.Input].value = ""
			case Failure(e) =>
				println(e.toString)
				g.alert(e.toString)
		}
	}

	/**
	  * Update the location showed in the google maps iframe
	  */
	def updateLocation(): Unit = {
		dom.document.getElementById("maps").setAttribute("src", "https://www.google.com/maps?q=" + dom.document.getElementById("location").asInstanceOf[html.Input].value + "&output=embed")
	}

	/**
	  * Notify participation to an event
	  */
	def participate(): Unit = {

		val url = "/events/" + dom.document.getElementById("eventId").getAttribute("value") + "/participation"
		val f = Ajax.post(url)
		f.onComplete {
			case Success(xhr) =>
				dom.document.getElementById("dontparticipate").setAttribute("class", "btn btn-danger pull-right ")
				dom.document.getElementById("participate").setAttribute("class", "btn btn-primary pull-right hidden")
			case Failure(e) =>
				println(e.toString)
				g.alert(e.toString)
		}
	}

	/**
	  * Cancel participation to an event
	  */
	def dontParticipate(): Unit = {
		val url = "/events/" + dom.document.getElementById("eventId").getAttribute("value") + "/participation"
		val f = Ajax.post(url)
		f.onComplete {
			case Success(xhr) =>
				dom.document.getElementById("participate").setAttribute("class", "btn btn-primary pull-right ")
				dom.document.getElementById("dontparticipate").setAttribute("class", "btn btn-danger pull-right hidden")
			case Failure(e) =>
				println(e.toString)
				g.alert(e.toString)
		}
	}
}
