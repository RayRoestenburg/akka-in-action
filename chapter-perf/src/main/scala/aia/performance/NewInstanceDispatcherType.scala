package aia.performance

import com.typesafe.config.Config
import akka.dispatch.Dispatcher
import akka.dispatch.MessageDispatcher
import akka.dispatch.MessageDispatcherConfigurator
import akka.dispatch.DispatcherPrerequisites
import java.util.concurrent.TimeUnit
import concurrent.duration.Duration

class NewInstanceDispatcherType(config: Config,
                                prerequisites: DispatcherPrerequisites)
  extends MessageDispatcherConfigurator(config, prerequisites) { //<co id="ch17-dispatcher-1" />

  override def dispatcher(): MessageDispatcher = { //<co id="ch17-dispatcher-2" />
    new Dispatcher( //<co id="ch17-dispatcher-3" />
      this,
      config.getString("id"),
      config.getInt("throughput"),
      Duration(config.getDuration("throughput-deadline-time", TimeUnit.NANOSECONDS), TimeUnit.NANOSECONDS),
      configureExecutor(),
      Duration(config.getDuration("shutdown-timeout", TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS))
  }
}