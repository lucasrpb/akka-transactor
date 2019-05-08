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

    for(i<-0 until 10){
      val b = 1000//rand.nextInt(0, 100000)
      accounts.put(i.toString, Account(b))
      moneyBefore = moneyBefore + (i.toString -> b)
    }

    val system = ActorSystem("bank")

    for(i<-0 until nactors){
      val s = system.actorOf(Props(classOf[Serializer], i.toString), s"${i}")
      actors.put(i.toString, s)
    }

    implicit val timeout = Timeout(10 seconds)

    var tasks = Seq.empty[Future[Boolean]]

    //val r = cli ? Start()

    val c1 = (system.actorOf(Props(classOf[Client], UUID.randomUUID.toString, "0", "1")) ? Start()).mapTo[Boolean]
    val c2 = (system.actorOf(Props(classOf[Client], UUID.randomUUID.toString, "2", "3")) ? Start()).mapTo[Boolean]
    val c3 = (system.actorOf(Props(classOf[Client], UUID.randomUUID.toString, "3", "4")) ? Start()).mapTo[Boolean]

    tasks = tasks ++ Seq(c1, c2, c3)

    val results = Await.result(Future.sequence(tasks), 10 seconds)

    println(s"results: ${results}\n")

    val mb = moneyBefore.map(_._2).sum
    val ma = accounts.map(_._2.balance).sum

    moneyBefore.keys.toSeq.sorted.foreach { id =>
      println(s"account $id before ${moneyBefore(id)} after ${accounts(id).balance}")
    }

    println(s"before: ${ma} moneyAfter ${mb}\n")

    assert(ma == mb)

    val f = CoordinatedShutdown(system).run(CoordinatedShutdown.UnknownReason)

    Await.ready(f, 60 seconds)
  }

}
