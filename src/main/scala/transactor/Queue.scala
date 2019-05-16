package transactor

import java.util.Collections
import java.util.concurrent.{ConcurrentLinkedDeque, ConcurrentLinkedQueue}

import scala.collection.concurrent.TrieMap

object Queue {

  val partitions = TrieMap[String, ConcurrentLinkedDeque[Transaction]](
    "0" -> new ConcurrentLinkedDeque[Transaction],
    "1" -> new ConcurrentLinkedDeque[Transaction],
    "2" -> new ConcurrentLinkedDeque[Transaction],
    "3" -> new ConcurrentLinkedDeque[Transaction],
    "4" -> new ConcurrentLinkedDeque[Transaction],
    "5" -> new ConcurrentLinkedDeque[Transaction],
  )

}
