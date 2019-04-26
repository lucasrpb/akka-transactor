package transactor

import java.util.UUID
import java.util.concurrent.ThreadLocalRandom

import akka.actor.{ActorRef, ActorSystem, Props}
import org.scalatest.FlatSpec

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

class MainSpec extends FlatSpec {

  "amount of money " should "be equal before and after transaction" in {

    val rand = ThreadLocalRandom.current()

    val system = ActorSystem("bank")

    val nactors = 3

    var actors = Map.empty[Int, ActorRef]

    for(i<-0 until nactors){
      val s = system.actorOf(Props(classOf[Serializer], i.toString), s"${i}")
      actors = actors + (i -> s)
    }

    val n = 400

    for(i<-0 until n){
      val id = rand.nextInt(0, 900)

      val msg = Enqueue(Transaction(id.toString))

      actors.foreach { case (_, ref) =>
        ref ! msg
      }
    }

    Await.ready(system.terminate(), 10 seconds)
  }

}
