package transactor

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.{AtomicBoolean, AtomicReference}

import akka.pattern._

import scala.concurrent.duration._
import akka.actor.Actor

class Serializer(val id: String) extends Actor {

  var queue = Seq.empty[Transaction]

  implicit val ec = context.dispatcher

  def execute(op: () => Unit): Unit = this.synchronized {
    op()
  }

  override def preStart(): Unit = {
    context.system.scheduler.schedule(100 milliseconds, 100 milliseconds){
      execute(() => {
        val list = queue.sortBy(_.id)

        println(s"executing in order at ${id}: ${list.map(_.id)}...\n")

        queue = Seq.empty[Transaction]
      })
    }
  }

  def enqueue(msg: transactor.Enqueue): Unit = {
    execute(() => {
      queue = queue :+ msg.t
      sender ! true
    })
  }

  override def receive: Receive = {
    case msg: Enqueue => enqueue(msg)
    case _ =>
  }
}
