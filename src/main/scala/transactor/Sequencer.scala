package transactor

import java.util
import java.util.{Collections, UUID}
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.{AtomicBoolean, AtomicReference}

import akka.pattern._

import scala.concurrent.duration._
import akka.actor.{Actor, Props}
import akka.util.Timeout

import scala.collection.concurrent.TrieMap

class Sequencer(val id: String) extends Actor {

  var batch = Seq.empty[Transaction]
  //val partition = Queue.partitions(id)

  implicit val ec = context.dispatcher

  def execute(op: () => Unit): Unit = this.synchronized {
    op()
  }

  val running = TrieMap[String, Transaction]()
  var list = Seq.empty[Transaction]

  override def preStart(): Unit = {
    context.system.scheduler.schedule(10 milliseconds, 10 milliseconds){
      execute(() => {

        val now = System.currentTimeMillis()

        list = list ++ batch
        batch = Seq.empty[Transaction]

        running.filterNot {case (id, t) => now - t.tmp >= TIMEOUT}

        var keys = running.map(_._2.keys).flatten.toSeq//Seq.empty[String]

        list = list.sortBy(_.id).filter { t =>

          val elapsed = now - t.tmp

          if(elapsed >= TIMEOUT){

            t.p.success(false)

            false
          } else if(!t.keys.exists(k => keys.contains(k))){

            keys = keys ++ t.keys
            running.put(t.id, t)

            t.p.success(true)

            false
          } else {
            true
          }
        }

      })
    }
  }

  def enqueue(msg: transactor.Enqueue): Unit = {
    execute(() => {
      batch = batch :+ msg.t
      msg.t.p.future.pipeTo(sender)
    })
  }

  def release(msg: transactor.Release): Unit = {
    execute(() => {
      running.remove(msg.t.id)
      sender ! true
    })
  }

  override def receive: Receive = {
    case msg: Release => release(msg)
    case msg: Enqueue => enqueue(msg)
    case _ =>
  }
}
