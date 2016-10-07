akka {
  loglevel = INFO
  stdout-loglevel = INFO
  loggers = ["akka.event.slf4j.Slf4jLogger"]
  logger-startup-timeout = 30s
  default-dispatcher {
    fork-join-executor {
      parallelism-min = 8
    }
  }
  test {
    timefactor = 1
  }
  http {
    server {
      server-header = "GoTicks.com REST API"
    }
  }
}

http {
  host = "0.0.0.0"
  host = ${?HOST}
  port = 5000
  port = ${?PORT}
}
