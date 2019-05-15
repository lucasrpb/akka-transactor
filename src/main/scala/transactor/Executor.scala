package transactor

import java.util.Comparator
import java.util.function.Consumer

import akka.actor.Actor
import akka.pattern._

import scala.collection.concurrent.TrieMap
import scala.concurrent.Future
import scala.concurrent.duration._

class Executor(val id: String) extends Actor {

  val partition = Queue.partitions(id)
  val running = TrieMap[String, Transaction]()
  var batch = Seq.empty[Transaction]

  implicit val ec = context.dispatcher

  def execute(op: () => Unit): Unit = this.synchronized {
    op()
  }

  def dequeue(): Unit = {

    if(!running.isEmpty){
      return
    }

    val batch = partition.poll()

    if(batch == null) return

    println(s"PROCESSING BATCH ${batch.id} txs: ${batch.txs.map(_.id)}...")

    var keys = Seq.empty[String]

    val now = System.currentTimeMillis()

    batch.txs.sortBy(_.id).foreach { t =>

      val elapsed = now - t.tmp

      if(t.keys.exists(k => keys.contains(k)) && elapsed < TIMEOUT){
        t.client ! AccessDenied(id)
      } else {

        keys = keys ++ t.keys
        running.put(t.id, t)

        t.client ! AccessGranted(id)
      }
    }
  }

  def release(cmd: transactor.Release): Unit = {
    println(s"RELEASING...")
    running.remove(cmd.t.id)

    sender ! true
  }

  override def preStart(): Unit = {
    context.system.scheduler.schedule(10 milliseconds, 10 milliseconds){
      execute(dequeue)
    }
  }

  override def receive: Receive = {
    //case cmd: Dequeue => dequeue()
    case cmd: Release => execute(() => {
      release(cmd)
    })
    case _ =>
  }
}
