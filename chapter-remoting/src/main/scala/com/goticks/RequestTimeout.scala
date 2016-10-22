package com.goticks

import akka.util.Timeout
import com.typesafe.config.Config

trait RequestTimeout {
  import scala.concurrent.duration._
  def configuredRequestTimeout(config: Config): Timeout = {
    val t = config.getString("akka.http.server.request-timeout")
    val d = Duration(t)
    FiniteDuration(d.length, d.unit)
  }
}
