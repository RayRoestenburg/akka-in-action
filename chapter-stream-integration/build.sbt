name := "integration"

version := "1.0"

organization := "manning"

libraryDependencies ++= {
  val akkaVersion = "2.5.4"
  val alpakkaVersion = "0.14"
  val akkaHttpVersion = "10.0.10"
  Seq(
    "org.scala-lang.modules"  %% "scala-xml" 				                 % "1.0.6",
    "com.typesafe.akka"       %% "akka-actor"                        % akkaVersion,
    "com.typesafe.akka"       %% "akka-slf4j"                        % akkaVersion,
    "com.typesafe.akka"       %% "akka-http"                         % akkaHttpVersion,
    "com.typesafe.akka"       %% "akka-http-spray-json"              % akkaHttpVersion,
    "com.typesafe.akka"       %% "akka-http-xml"                     % akkaHttpVersion,
    "com.typesafe.akka"       %% "akka-stream"                       % akkaVersion,
    "com.lightbend.akka"      %% "akka-stream-alpakka-file"          % alpakkaVersion,
    "com.lightbend.akka"      %% "akka-stream-alpakka-amqp"          % alpakkaVersion,
    "ch.qos.logback"          %  "logback-classic"                   % "1.1.3",
    "commons-io"              %  "commons-io"                        % "2.0.1"         % "test",
    "io.arivera.oss"          %  "embedded-rabbitmq"                 % "1.2.1"         % "test",
    "com.typesafe.akka"       %% "akka-http-testkit"                 % akkaHttpVersion % "test",
    "com.typesafe.akka"       %% "akka-stream-testkit"               % akkaVersion     % "test",
    "com.typesafe.akka"       %% "akka-testkit"                      % akkaVersion     % "test",
    "org.scalatest"           %% "scalatest"                         % "3.0.0"         % "test"
  )
}

