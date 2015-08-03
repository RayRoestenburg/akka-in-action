import com.typesafe.sbt.SbtNativePackager._
import NativePackagerHelper._
import NativePackagerKeys._

name := "deploy"

version := "0.1-SNAPSHOT"

organization := "manning"

scalaVersion := "2.11.7"

scalacOptions ++= Seq(
  "-deprecation",
  "-unchecked",
  "-Xlint",
  "-Ywarn-unused",
  "-Ywarn-dead-code",
  "-feature",
  "-language:_"
)

packageArchetype.java_application

scriptClasspath +="../conf"

libraryDependencies ++= {
  val akkaVersion       = "2.3.12"
  Seq(
    "com.typesafe.akka" %%  "akka-actor"      % akkaVersion,
    "com.typesafe.akka" %%  "akka-slf4j"      % akkaVersion,
    "ch.qos.logback"     %  "logback-classic" % "1.0.13",
    "com.typesafe.akka" %%  "akka-testkit"    % akkaVersion   % "test",
    "org.scalatest"     %%  "scalatest"       % "2.2.4"       % "test"
  )
}

