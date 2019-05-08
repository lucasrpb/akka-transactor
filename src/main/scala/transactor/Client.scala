package transactor

import java.util.UUID
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.atomic.AtomicInteger

import akka.actor.Actor
import akka.pattern._

import scala.concurrent.duration._
import akka.util.Timeout

import scala.concurrent.{Future, Promise}

class Client(val id: String, a1: String, a2: String) extends Actor {

  import Global._
  implicit val timeout = Timeout(10 seconds)

  val rand = ThreadLocalRandom.current()

  val p = Promise[Boolean]()
  var counter = new AtomicInteger()

  var partitions = Map.empty[String, Transaction]

  implicit val ec = context.dispatcher

  def execute(): Unit = {

    val n = counter.incrementAndGet()

    if(n < partitions.size) return

    val acc1 = accounts(a1)
    val acc2 = accounts(a2)

    var b1 = acc1.balance
    var b2 = acc2.balance

    if(b1 == 0) {
      println(s"no money to transfer")
      p.success(true)
      return
    }

    val ammount = rand.nextInt(0, b1)

    b1 = b1 - ammount
    b2 = b2 + ammount

    acc1.balance = b1
    acc2.balance = b2

    p.success(true)
  }

  def start(): Unit = {

    val keys = Seq(a1, a2)

    println(s"tx ${id} keys ${keys}\n")

    keys.foreach { k =>
      val p = (k.toInt % actors.size).toString

      if(partitions.isDefinedAt(p)){
        val t = partitions(p)
        t.keys = t.keys :+ k
      } else {
        partitions = partitions + (p -> Transaction(id, Seq(k), self))
      }
    }

    //println(s"partitions for tx $id ${partitions.map(_._1)}\n")

    partitions.foreach { case (p, t) =>
      actors(p) ! Enqueue(t)
    }
  }

  override def receive: Receive = {
    case cmd: Start =>

      p.future.flatMap { r =>

        Future.sequence(partitions.map { case (id, t) =>
          executors(id) ? Release(t)
        }).map(_ => r)

      }.pipeTo(sender)

      start()

    case cmd: AccessGranted => execute()
    case cmd: AccessDenied =>

      p.trySuccess(false)

    case _ =>
  }
}
