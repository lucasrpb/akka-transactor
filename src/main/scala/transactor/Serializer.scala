package transactor

import java.util
import java.util.{Collections, UUID}
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.{AtomicBoolean, AtomicReference}

import akka.pattern._

import scala.concurrent.duration._
import akka.actor.{Actor, Props}
import akka.util.Timeout

class Serializer(val id: String) extends Actor {

  var batch = Seq.empty[Transaction]
  val partition = Queue.partitions(id)

  implicit val ec = context.dispatcher

  def execute(op: () => Unit): Unit = this.synchronized {
    op()
  }

  override def preStart(): Unit = {
    context.system.scheduler.schedule(100 milliseconds, 100 milliseconds){
      execute(() => {

        //val list = batch.sortBy(_.id)

        if(!batch.isEmpty) {
          val b = Batch(UUID.randomUUID.toString, batch)

          println(s"ADDING BATCH ${b.id}...")

          partition.add(b)
        }

        batch = Seq.empty[Transaction]

      })
    }
  }

  def enqueue(msg: transactor.Enqueue): Unit = {
    execute(() => {
      batch = batch :+ msg.t
    })
  }

  override def receive: Receive = {
    case msg: Enqueue => enqueue(msg)
    case _ =>
  }
}
