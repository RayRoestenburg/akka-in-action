import AssemblyKeys._
import com.typesafe.sbt.SbtStartScript

name := "goticks"

version := "0.1-SNAPSHOT"

organization := "com.goticks"

scalaVersion := "2.10.4"

resolvers ++= Seq("Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
                  "Sonatype snapshots"  at "http://oss.sonatype.org/content/repositories/snapshots/",
                  "Spray Repository"    at "http://repo.spray.io",
                  "Spray Nightlies"     at "http://nightlies.spray.io/")

libraryDependencies ++= {
  val akkaVersion       = "2.3.3"
  val sprayVersion      = "1.3.1"
  Seq(
    "com.typesafe.akka" %% "akka-actor"      % akkaVersion,
    "io.spray"          %  "spray-can"       % sprayVersion,
    "io.spray"          %  "spray-routing"   % sprayVersion,
    "io.spray"          %% "spray-json"      % "1.2.3",
    "com.typesafe.akka" %% "akka-slf4j"      % akkaVersion,
    "ch.qos.logback"    %  "logback-classic" % "1.0.10",
    "com.typesafe.akka" %%  "akka-testkit"   % akkaVersion   % "test",
    "org.scalatest"     %% "scalatest"       % "1.9.1"       % "test",
    // for java examples
    "com.google.guava"  % "guava"            % "14.0"
  )
}

// Assembly settings
mainClass in Global := Some("com.goticks.Main")

jarName in assembly := "goticks-server.jar"

assemblySettings

// StartScript settings
seq(SbtStartScript.startScriptForClassesSettings: _*)
