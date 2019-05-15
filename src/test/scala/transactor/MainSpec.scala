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
      val s = system.actorOf(Props(classOf[Serializer], i.toString), s"s-${i}")
      val e = system.actorOf(Props(classOf[Executor], i.toString), s"e-${i}")

      sequencers.put(i.toString, s)
      executors.put(i.toString, e)
    }

    implicit val timeout = Timeout(TIMEOUT milliseconds)

    var tasks = Seq.empty[Future[Boolean]]

    /*val c1 = (system.actorOf(Props(classOf[Client], UUID.randomUUID.toString, "0", "1")) ? Start()).mapTo[Boolean]
    val c2 = (system.actorOf(Props(classOf[Client], UUID.randomUUID.toString, "2", "3")) ? Start()).mapTo[Boolean]
    val c3 = (system.actorOf(Props(classOf[Client], UUID.randomUUID.toString, "3", "4")) ? Start()).mapTo[Boolean]

    tasks = tasks ++ Seq(c1, c2, c3)*/

    for(i<-0 until 100){
      val a1 = rand.nextInt(0, accounts.size).toString
      val a2 = rand.nextInt(0, accounts.size).toString

      if(!a1.equals(a2)) {
        val id = i.toString//UUID.randomUUID.toString
        val t0 = System.currentTimeMillis()
        val c =  (system.actorOf(Props(classOf[Client], id, a1, a2)) ? Start()).mapTo[Boolean]
          .map { r =>

            println(s"\nTX $id FINISHED => $r! elapsed ${System.currentTimeMillis() - t0}ms")

            r
          }.recover {case _ => false}

        tasks = tasks :+ c
      }
    }

    val results = Await.result(Future.sequence(tasks), 5 seconds)

    println(s"\nn: ${results.length} successes: ${results.count(_ == true)}\n")

    val mb = moneyBefore.map(_._2).sum
    val ma = accounts.map(_._2.balance).sum

    /*moneyBefore.keys.toSeq.sorted.foreach { id =>
      println(s"account $id before ${moneyBefore(id)} after ${accounts(id).balance}")
    }*/

    println(s"before: ${ma} moneyAfter ${mb}\n")

    assert(ma == mb)

    val f = CoordinatedShutdown(system).run(CoordinatedShutdown.UnknownReason)

    Await.ready(f, 10 seconds)
  }

}
