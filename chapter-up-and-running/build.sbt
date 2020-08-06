enablePlugins(JavaAppPackaging)
import Dependencies._

name := "goticks"

version := "1.0"

organization := "com.goticks" 

libraryDependencies ++= {
  val akkaVersion = "2.6.3"
  val akkaHttpVersion = "10.1.12"

  // https://mvnrepository.com/artifact/com.typesafe.akka/akka-http-core
    //libraryDependencies += "com.typesafe.akka" %% "akka-http-core" %

  Seq(
    "com.typesafe.akka" %% "akka-actor"      % akkaVersion, 
    "com.typesafe.akka" %% "akka-http-core"  % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-http"  % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-http-spray-json"  % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-stream" % akkaVersion,
    "com.typesafe.akka" %% "akka-slf4j"      % akkaVersion,
    "ch.qos.logback"    %  "logback-classic" % "1.1.3",
    "com.typesafe.akka" %% "akka-testkit"    % akkaVersion   % "test",
    //"org.scalatest"     %% "scalatest"       % "2.2.0"       % "test"

  )
}

// Assembly settings
mainClass in assembly := Some("com.goticks.Main")

assemblyJarName in assembly := "goticks.jar"