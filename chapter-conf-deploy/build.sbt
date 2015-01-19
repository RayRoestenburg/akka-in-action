import com.typesafe.sbt.SbtNativePackager._
import NativePackagerHelper._

name := "deploy"

version := "0.1-SNAPSHOT"

organization := "manning"

scalaVersion := "2.11.1"

packageArchetype.java_application

libraryDependencies ++= {
  val akkaVersion       = "2.3.4"
  Seq(
    "com.typesafe.akka"       %%  "akka-actor"                     % akkaVersion,
    "com.typesafe.akka"       %%  "akka-slf4j"                     % akkaVersion,
    "com.typesafe.akka"       %%  "akka-testkit"                   % akkaVersion   % "test",
    "org.scalatest"           %% "scalatest"                       % "2.2.0"       % "test"
  )
}
