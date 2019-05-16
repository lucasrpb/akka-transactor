import akka.actor.ActorRef

import scala.concurrent.Promise

package object transactor {

  val TIMEOUT = 150

  trait Command

  trait Message

  case class Batch(val id: String, txs: Seq[Transaction])

  case class Transaction(id: String, var keys: Seq[String],
                         tmp: Long = System.currentTimeMillis(),
                         p: Promise[Boolean] = Promise[Boolean]())

  case class Enqueue(t: Transaction) extends Message

  case class Release(t: Transaction) extends Message

  case class Dequeue() extends Command

  case class Start() extends Command

  case class AccessDenied(id: String) extends Command
  case class AccessGranted(id: String) extends Command
}
