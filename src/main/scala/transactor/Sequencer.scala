package transactor

import java.util
import java.util.{Collections, UUID}
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.{AtomicBoolean, AtomicInteger, AtomicReference}

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

  var queue = Seq.empty[Transaction]

  val running = TrieMap[String, Transaction]()
  var work = Seq.empty[Transaction]

  var pos = 0

  val BATCH_SIZE = 20

  override def preStart(): Unit = {
    context.system.scheduler.schedule(10 milliseconds, 10 milliseconds){
      execute(() => {

        val now = System.currentTimeMillis()

        queue = queue ++ batch
        batch = Seq.empty[Transaction]

        work = work.filter { case t =>

          val elapsed = now - t.tmp

          if(elapsed >= TIMEOUT){
            t.p.success(false)
            false
          } else {
            true
          }
        }

        val count = BATCH_SIZE - work.size

        if(count > 0){
          val slice = queue.slice(pos, pos + count)
          work = work ++ slice
          pos += count
        }

        var keys = running.map(_._2.keys).flatten.toSeq

        val remove = work.sortBy(_.id).filter { t =>

          val elapsed = now - t.tmp

          if(elapsed >= TIMEOUT){
            t.p.success(false)
            true
          } else if(!t.keys.exists(k => keys.contains(k))) {

            keys = keys ++ t.keys
            running.put(t.id, t)

            t.p.success(true)

            true
          } else {
            false
          }

        }

        work = work.filterNot(t => remove.exists(_.id.equals(t.id)))

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
