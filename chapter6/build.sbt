import AssemblyKeys._
import com.typesafe.startscript.StartScriptPlugin

seq(StartScriptPlugin.startScriptForClassesSettings: _*)

name := "goticks"

version := "0.1-SNAPSHOT"

organization := "com.goticks"

scalaVersion := "2.10.0"

resolvers ++= Seq("Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
                  "Sonatype snapshots"  at "http://oss.sonatype.org/content/repositories/snapshots/",
                  "Spray Repository"    at "http://repo.spray.io",
                  "Spray Nightlies"     at "http://nightlies.spray.io/")

libraryDependencies ++= {
  val akkaVersion       = "2.2-M3"
  val sprayVersion      = "1.2-M8-SNAPSHOT"
  Seq(
    "com.typesafe.akka"       %%  "akka-actor"                     % akkaVersion,
    "com.typesafe.akka"       %%  "akka-slf4j"                     % akkaVersion,
    "com.typesafe.akka"       %%  "akka-remote"                    % akkaVersion,
    "com.typesafe.akka"       %%  "akka-cluster-experimental"      % akkaVersion,
    "com.typesafe.akka"       %%  "akka-multi-node-testkit"        % akkaVersion   % "test",
    "com.typesafe.akka"       %%  "akka-testkit"                   % akkaVersion   % "test",
    "org.scalatest"           %% "scalatest"                       % "1.9.1"       % "test",
    "io.spray"                %  "spray-can"                       % sprayVersion,
    "io.spray"                %  "spray-routing"                   % sprayVersion,
    "io.spray"                %% "spray-json"                      % "1.2.3",
    "com.typesafe.akka"       %% "akka-slf4j"                      % akkaVersion,
    "ch.qos.logback"          %  "logback-classic"                 % "1.0.10"
  )
}

// Assembly settings
mainClass in Global := Some("com.goticks.Main")

jarName in assembly := "goticks-server.jar"

assemblySettings
