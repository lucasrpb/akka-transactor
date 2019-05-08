import akka.actor.ActorRef

package object transactor {

  trait Command

  trait Message

  case class Batch(txs: Seq[Transaction])

  case class Transaction(id: String, var keys: Seq[String], client: ActorRef,
                         tmp: Long = System.currentTimeMillis())

  case class Enqueue(t: Transaction) extends Message

  case class Release(t: Transaction) extends Message

  case class Dequeue() extends Command

  case class Start() extends Command

  case class AccessDenied(id: String) extends Command
  case class AccessGranted(id: String) extends Command
}
