name := "goticks"

version := "1.0"

organization := "com.goticks"

libraryDependencies ++= {
  val akkaVersion = "2.4.19"
  val akkaHttpVersion ="10.0.9"
  Seq(
    "com.typesafe.akka" %%  "akka-actor"              % akkaVersion,
    "com.typesafe.akka" %%  "akka-slf4j"              % akkaVersion,

    "com.typesafe.akka" %%  "akka-remote"             % akkaVersion,
    "com.typesafe.akka" %%  "akka-multi-node-testkit" % akkaVersion % "test",

    "com.typesafe.akka" %%  "akka-testkit"            % akkaVersion % "test",
    "org.scalatest"     %%  "scalatest"               % "3.0.0"     % "test",
    "com.typesafe.akka" %%  "akka-http-core"          % akkaHttpVersion,
    "com.typesafe.akka" %%  "akka-http"               % akkaHttpVersion,
    "com.typesafe.akka" %%  "akka-http-spray-json"    % akkaHttpVersion,
    "ch.qos.logback"    %   "logback-classic"         % "1.1.6"
  )
}

// Assembly settings
mainClass in assembly := Some("com.goticks.SingleNodeMain")

assemblyJarName in assembly := "goticks-server.jar"
