package object transactor {

  trait Command

  trait Message

  case class Batch(txts: Seq[Transaction])

  case class Transaction(id: String, tmp: Long = System.currentTimeMillis())

  case class Enqueue(t: Transaction) extends Message

}
