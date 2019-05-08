package transactor

import akka.actor.Actor
import akka.pattern._

import scala.collection.concurrent.TrieMap
import scala.concurrent.duration._

class Executor(val id: String) extends Actor {

  val partition = Queue.partitions(id)
  var running = TrieMap[String, Transaction]()

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

    var keys = Seq.empty[String]

    batch.txs.foreach { t =>
      if(t.keys.exists(k => keys.contains(k))){
        t.client ! AccessDenied(id)
      } else {

        running.put(t.id, t)

        keys = keys ++ t.keys
        t.client ! AccessGranted(id)
      }
    }
  }

  def release(cmd: transactor.Release): Unit = {
    execute(() => {
      running.remove(cmd.t.id)
    })
  }

  /*override def preStart(): Unit = {
    context.system.scheduler.schedule(100 milliseconds, 100 milliseconds){
      execute(dequeue)
    }
  }*/

  override def receive: Receive = {
    case cmd: Dequeue => execute(dequeue)
    case cmd: Release => release(cmd)
    case _ =>
  }
}
