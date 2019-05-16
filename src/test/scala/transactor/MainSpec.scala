package transactor

import java.util.UUID
import java.util.concurrent.ThreadLocalRandom

import akka.actor.{ActorRef, ActorSystem, CoordinatedShutdown, Props}
import org.scalatest.FlatSpec
import akka.pattern._
import akka.util.Timeout

import scala.collection.concurrent.TrieMap
import scala.concurrent.{Await, Future, Promise}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

class MainSpec extends FlatSpec {

  "amount of money " should "be equal before and after transaction" in {

    val rand = ThreadLocalRandom.current()

    import Global._
    var moneyBefore = Map.empty[String, Int]

    for(i<-0 until 100){
      val b = rand.nextInt(1000, 10000)
      accounts.put(i.toString, Account(b))
      moneyBefore = moneyBefore + (i.toString -> b)
    }

    val system = ActorSystem("bank")

    for(i<-0 until 3){
      val s = system.actorOf(Props(classOf[Sequencer], i.toString), s"s-${i}")
      //val e = system.actorOf(Props(classOf[Executor], i.toString), s"e-${i}")

      sequencers.put(i.toString, s)
      //executors.put(i.toString, e)
    }

    implicit val timeout = Timeout(TIMEOUT milliseconds)

    var tasks = Seq.empty[Future[Boolean]]

    def transfer(i: Int, a1: String, a2: String): Future[Boolean] = {
      val id = i.toString//UUID.randomUUID.toString
      val tmp = System.currentTimeMillis()

      val acc1 = accounts(a1)
      val acc2 = accounts(a2)

      val keys = Seq(a1, a2)
      var partitions = Map.empty[String, Transaction]

      keys.foreach { k =>
        val p = (k.toInt % sequencers.size).toString

        if(partitions.isDefinedAt(p)){
          val t = partitions(p)
          t.keys = t.keys :+ k
        } else {
          partitions = partitions + (p -> Transaction(id, Seq(k), tmp))
        }
      }

      val locks = partitions.map { case (p, t) =>
        sequencers(p) ? Enqueue(t)
      }.map(_.mapTo[Boolean])

      Future.sequence(locks).map { results =>

        if(results.exists(_ == false)){
          false
        } else {

          var b1 = acc1.balance
          var b2 = acc2.balance

          if(b1 == 0) {
            println(s"no money to transfer")
          } else {
            val ammount = rand.nextInt(0, b1)

            b1 = b1 - ammount
            b2 = b2 + ammount

            acc1.balance = b1
            acc2.balance = b2
          }

          true
        }
      }.recover{case _ => false}
        .map { r =>

          partitions.foreach { case (p, t) =>
            sequencers(p) ! Release(t)
          }

          println(s"\nTX DONE ${id} => ${r}")

          r
        }
    }

    for(i<-0 until 100){
      val a1 = rand.nextInt(0, accounts.size).toString
      val a2 = rand.nextInt(0, accounts.size).toString

      if(!a1.equals(a2)) {
        tasks = tasks :+ transfer(i, a1, a2)
      }
    }

    val results = Await.result(Future.sequence(tasks), 5 seconds)

    val n = results.length
    val hits = results.count(_ == true)
    val rate = (hits/n.toDouble)*100

    println(s"\nn: ${n} successes: ${hits} rate: ${rate}%\n")

    val mb = moneyBefore.map(_._2).sum
    val ma = accounts.map(_._2.balance).sum

    /*moneyBefore.keys.toSeq.sorted.foreach { id =>
      println(s"account $id before ${moneyBefore(id)} after ${accounts(id).balance}")
    }*/

    println(s"before: ${ma} after ${mb}\n")

    assert(ma == mb)

    val f = CoordinatedShutdown(system).run(CoordinatedShutdown.UnknownReason)

    Await.ready(f, 10 seconds)
  }

}
