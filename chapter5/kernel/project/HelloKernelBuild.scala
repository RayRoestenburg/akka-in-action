
import sbt._
import Keys._
import akka.sbt.AkkaKernelPlugin
import akka.sbt.AkkaKernelPlugin.{ Dist,
    outputDirectory, distJvmOptions}
 
object HelloKernelBuild extends Build {
 
  lazy val HelloKernel = Project(
    id = "hello-kernel-book",
    base = file("."),
    settings = defaultSettings ++ AkkaKernelPlugin.distSettings
      ++ Seq(
      libraryDependencies ++= Dependencies.helloKernel,
      distJvmOptions in Dist := "-Xms256M -Xmx1024M",
      outputDirectory in Dist := file("target/helloDist")
    )
  )
 
  lazy val buildSettings = Defaults.defaultSettings ++ Seq(
    organization := "com.manning",
    version      := "0.1-SNAPSHOT",
    scalaVersion := "2.9.1",
    crossPaths   := false,
    organizationName := "Mannings",
    organizationHomepage := Some(url("http://www.mannings.com"))
  )
  
  lazy val defaultSettings = buildSettings ++ Seq(
    resolvers += "Typesafe Repo" at
      "http://repo.typesafe.com/typesafe/releases/",
 
    // compile options
    scalacOptions ++= Seq("-encoding", "UTF-8",
      "-deprecation",
      "-unchecked"),
    javacOptions  ++= Seq("-Xlint:unchecked",
      "-Xlint:deprecation")
 
  )
}
// Dependencies

object Dependencies {
  import Dependency._
  val helloKernel = Seq(akkaActor,
    akkaKernel,
    akkaSlf4j,
    slf4jApi,
    slf4jLog4j,
    Test.junit,
    Test.scalatest,
    Test.akkaTestKit)
}

object Dependency {
  
  // Versions
  object V {
    val Scalatest    = "1.6.1"
    val Slf4j        = "1.6.4"
    val Akka         = "2.0"
  }

  // Compile
  val commonsCodec  = "commons-codec"     % "commons-codec"% "1.4"
  val commonsIo     = "commons-io"        % "commons-io"   % "2.0.1"
  val commonsNet    = "commons-net"       % "commons-net"  % "3.1"
  val slf4jApi      = "org.slf4j"         % "slf4j-api"    % V.Slf4j
  val slf4jLog4j    = "org.slf4j"         % "slf4j-log4j12"% V.Slf4j
  val akkaActor     = "com.typesafe.akka" % "akka-actor"   % V.Akka
  val akkaKernel    = "com.typesafe.akka" % "akka-kernel"  % V.Akka
  val akkaSlf4j     = "com.typesafe.akka" % "akka-slf4j"   % V.Akka
  val scalatest     = "org.scalatest"     %% "scalatest" % V.Scalatest

  object Test {
    val junit       = "junit" % "junit" %
                      "4.5"        % "test"
    val scalatest   = "org.scalatest"     %% "scalatest" %
                      V.Scalatest  % "test"
    val akkaTestKit = "com.typesafe.akka" % "akka-testkit" %
                      V.Akka % "test"
  }
}
