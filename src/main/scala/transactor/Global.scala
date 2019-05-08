package transactor

import akka.actor.{ActorRef, Props}

import scala.collection.concurrent.TrieMap

object Global {

  val nactors = 3

  case class Account(var balance: Int = 0)

  val accounts = TrieMap[String, Account]()
  val actors = TrieMap[String, ActorRef]()

}
