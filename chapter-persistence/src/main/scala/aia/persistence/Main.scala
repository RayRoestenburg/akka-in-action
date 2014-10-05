package aia.persistence

import akka.actor._

object Main extends App {
  val system = ActorSystem("calc")
  val calc = system.actorOf(PersistentCalculator.props, PersistentCalculator.name)

  calc ! PersistentCalculator.Add(1)
  calc ! PersistentCalculator.Multiply(3)
  calc ! PersistentCalculator.Divide(4)
  calc ! PersistentCalculator.PrintResult
}
