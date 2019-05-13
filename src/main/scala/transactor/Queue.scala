package transactor

import java.util.Collections
import java.util.concurrent.{ConcurrentLinkedDeque, ConcurrentLinkedQueue}

import scala.collection.concurrent.TrieMap

object Queue {

  val partitions = TrieMap[String, ConcurrentLinkedDeque[Batch]](
    "0" -> new ConcurrentLinkedDeque[Batch],
    "1" -> new ConcurrentLinkedDeque[Batch],
    "2" -> new ConcurrentLinkedDeque[Batch]
  )

}
