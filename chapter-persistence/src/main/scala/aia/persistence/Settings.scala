package aia.persistence

import scala.concurrent.duration._

import akka.actor._

import com.typesafe.config.Config

object Settings extends ExtensionKey[Settings]

class Settings(config: Config) extends Extension {
  def this(system: ExtendedActorSystem) = this(system.settings.config)

  val passivateTimeout = Duration(config.getString("passivate-timeout"))
  object http {
    val host = config.getString("http.host")
    val port = config.getInt("http.port")
  }
}
