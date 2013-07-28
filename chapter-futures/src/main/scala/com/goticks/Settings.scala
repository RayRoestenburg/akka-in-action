package com.goticks

import com.typesafe.config.Config
import akka.actor.{ExtensionIdProvider, ExtensionId, Extension, ExtendedActorSystem}

class Settings(config: Config, extendedSystem: ExtendedActorSystem) extends Extension {

  object Http {
    val Host = config.getString("ticket-info-service.http.host")
    val Port = config.getInt("ticket-info-service.http.port")
  }
}

object Settings extends ExtensionId[Settings] with ExtensionIdProvider {

  override def lookup = Settings

  override def createExtension(system: ExtendedActorSystem) =
    new Settings(system.settings.config, system)

}


