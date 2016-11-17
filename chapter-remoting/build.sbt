name := "goticks"

version := "1.0"

organization := "com.goticks"

libraryDependencies ++= {
  val akkaVersion = "2.4.12"
  Seq(
    "com.typesafe.akka" %%  "akka-actor"              % akkaVersion,
    "com.typesafe.akka" %%  "akka-slf4j"              % akkaVersion,

    "com.typesafe.akka" %%  "akka-remote"             % akkaVersion,
    "com.typesafe.akka" %%  "akka-multi-node-testkit" % akkaVersion % "test",

    "com.typesafe.akka" %%  "akka-testkit"                       % akkaVersion % "test",
    "org.scalatest"     %%  "scalatest"                          % "3.0.0"     % "test",
    "com.typesafe.akka" %%  "akka-http-core"                     % "2.4.11", 
    "com.typesafe.akka" %%  "akka-http-experimental"             % "2.4.11", 
    "com.typesafe.akka" %%  "akka-http-spray-json-experimental"  % "2.4.11", 
    "ch.qos.logback"    %   "logback-classic"                    % "1.1.6"
  )
}

// Assembly settings
mainClass in Global := Some("com.goticks.SingleNodeMain")

jarName in assembly := "goticks-server.jar"
