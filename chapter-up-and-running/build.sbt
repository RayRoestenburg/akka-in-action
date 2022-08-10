enablePlugins(JavaServerAppPackaging)

name := "goticks"

version := "1.0"

organization := "com.goticks"

libraryDependencies ++= {
  val akkaVersion = "2.6.19"
  Seq(
    "com.typesafe.akka" %% "akka-actor"      % akkaVersion,
    "com.typesafe.akka" %% "akka-http-core" % "10.2.9",
    "com.typesafe.akka" %% "akka-slf4j"      % akkaVersion,
    "com.typesafe.akka" %% "akka-stream" % akkaVersion,
    "ch.qos.logback"    %  "logback-classic" % "1.2.11",
    "com.typesafe.akka" %% "akka-testkit"    % akkaVersion   % "test",
    "org.scalatest" %% "scalatest" % "3.2.12" % "test",
    "org.scalameta" %% "munit" % "0.7.29" % "test"
  )
}

val AkkaHttpVersion = "10.2.9"
libraryDependencies += "com.typesafe.akka" %% "akka-http-spray-json" % AkkaHttpVersion

// Assembly settings
mainClass in assembly := Some("com.goticks.Main")

assemblyJarName in assembly := "goticks.jar"