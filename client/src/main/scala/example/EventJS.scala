package example

import org.scalajs.dom
import org.scalajs.dom.ext.Ajax
import org.scalajs.dom.html
import org.scalajs.dom.raw.HTMLFormElement
import shared.SharedMessages

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * THINGS THAT DOESNT WORK
  * - Template project with React
  * - build.sbt add google maps, jquery and datepicker
  * - Slick not working
  * - Timestamp in Slick
  * - ScalaJS doccument ready
  * - ScalaJS getElementById... Script placé au mauvais endroit dans la page
  * - Récupérer la valeur d'un simple textarea ou input...
  * - Ecrire HTML depuis scalajs
  * - Gestion des erreurs dans le frontend (futur)
  * - 404 NOT FOUND
  */

import scala.scalajs.js
import scala.util.{Failure, Success}
import js.Dynamic.{ global => g }

object EventJS extends js.JSApp {
  def main(): Unit = {
    dom.document.getElementById("send").addEventListener("click", { (e0: dom.Event) =>
      sendMessage
    }, false)
  }
  def sendMessage(): Unit = {
    val url = "/events/" + dom.document.getElementById("eventId").getAttribute("value") + "/messages"
    val f = Ajax.post(url, data = dom.document.getElementById("message").asInstanceOf[html.Input].value)
    f.onComplete{
      case Success(xhr) =>
        dom.document.getElementById("messages").innerHTML = " <div class=\"message\">\n" +
          "<p>" +  dom.document.getElementById("message").asInstanceOf[html.Input].value + "</p>\n" +
          "<i class=\"pull-right\">Created by @message.creator</i>\n" +
          "<i>Sent at @message.date</i>\n" +
          "</div>\n" +
          "<hr/>" +
          dom.document.getElementById("messages").innerHTML
        dom.document.getElementById("message").asInstanceOf[html.Input].value = ""
      case Failure(e) =>
        println(e.toString)
        g.alert(e.toString)
    }
  }
}
