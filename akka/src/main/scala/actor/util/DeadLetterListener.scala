package actor.util

import akka.actor.{DeadLetter, Actor}
import akka.event.Logging
import msg.Work

/**
 * Created by bartosz on 21.01.15.
 */
class DeadLetterListener extends Actor {
  val log = Logging(context.system, this)

  override def receive: Receive = {
    case DeadLetter(msg : Work, from, to) => log.debug("DL {} to {}", msg.id, to.path)
    case DeadLetter(msg, from, to) => log.debug("DL [{}] to {}", msg, to.path)
  }
}
