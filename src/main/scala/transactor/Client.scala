package transactor

import akka.actor.Actor

class Client(val id: String) extends Actor {

  override def receive: Receive = {
    case _ =>
  }
}
