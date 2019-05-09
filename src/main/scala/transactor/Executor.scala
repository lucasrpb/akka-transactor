package transactor

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

  def process(): Unit = {
    var txs = Seq.empty[Transaction]

    batch.foreach { t =>

      val keys = running.values.map(_.keys).flatten.toSeq

      if(!t.keys.exists(k => keys.contains(k))){
        running.put(t.id, t)
        txs = txs :+ t
      }
    }

    batch = batch.filterNot(t => txs.exists(_.id.equals(t.id)))

    txs.foreach(_.client ! AccessGranted(id))

    println(s"batch ${batch.map(_.id)} size ${batch.size}\n")
  }

  def dequeue(): Unit = {

    /*if(!running.isEmpty){
      return
    }*/

    if(batch.isEmpty){
      val b = partition.poll()

      if(b == null) return

      batch = b.txs
    }

    process()
  }

  def release(cmd: transactor.Release): Unit = {
    println(s"RELEASING...")
    running.remove(cmd.t.id)

    sender ! true
  }

  override def preStart(): Unit = {
    context.system.scheduler.schedule(100 milliseconds, 100 milliseconds){
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
