package test

import akka.actor.Actor
import akka.event.Logging

/**
 * Created by bartosz on 20.01.15.
 */
class MyActor extends Actor {
  val log = Logging(context.system, this)
  def receive = {
    case a : String => log.info("MSG: " + a  + " from " + sender())
    case _ => log.warning("WTF??" + " from " + sender())
  }
}
