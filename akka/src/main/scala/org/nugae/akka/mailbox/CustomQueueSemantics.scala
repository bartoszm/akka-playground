package org.nugae.akka.mailbox

import java.util.concurrent.{TimeUnit, BlockingQueue, LinkedBlockingQueue}

import akka.actor.{ActorSystem, ActorRef}
import akka.dispatch._
import com.typesafe.config.Config

import scala.concurrent.duration.FiniteDuration

/**
 * Created by bartosz on 21.01.15.
 */
trait CustomQueueSemantics
trait SomeOtherQueueSemantics extends CustomQueueSemantics

/**
 * BoundedMailbox is the default bounded MailboxType used by Akka Actors.
 */
case class CustomBoundedMailbox(val capacity: Int, val pushTimeOut: FiniteDuration)
  extends MailboxType with ProducesMessageQueue[CustomBoundedMailbox.MessageQueue] {

  def this(settings: ActorSystem.Settings, config: Config) = this(config.getInt("mailbox-capacity"),
    new FiniteDuration(
    config.getDuration("mailbox-push-timeout-time", TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS))

  if (capacity < 0) throw new IllegalArgumentException("The capacity for BoundedMailbox can not be negative")
  if (pushTimeOut eq null) throw new IllegalArgumentException("The push time-out for BoundedMailbox can not be null")

  override def create(owner: Option[ActorRef], system: Option[ActorSystem]): MessageQueue =
    new CustomBoundedMailbox.MessageQueue(capacity, pushTimeOut)
}

object CustomBoundedMailbox {
  class MessageQueue(capacity: Int, final val pushTimeOut: FiniteDuration) extends LinkedBlockingQueue[Envelope](capacity)
          with BoundedQueueBasedMessageQueue with SomeOtherQueueSemantics {
    final def queue: BlockingQueue[Envelope] = this
  }
}
