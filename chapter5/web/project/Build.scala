import sbt._
import Keys._
import PlayProject._

object Build extends Build {
  lazy val root = Project(id = "playminiHello",
  	base = file("."), settings = Project.defaultSettings).settings(
    resolvers += "Typesafe Repo" at
      "http://repo.typesafe.com/typesafe/releases/",
    resolvers += "Typesafe Snapshot Repo" at
      "http://repo.typesafe.com/typesafe/snapshots/",
    libraryDependencies ++= Dependencies.hello,
    mainClass in (Compile, run) :=       //<co id="ch04-web-build-0"/>
      Some("play.core.server.NettyServer"))
}

object Dependencies {
  import Dependency._
  val hello = Seq(akkaActor,
    akkaSlf4j,
   // slf4jLog4j,
    playmini
  )
}

object Dependency {

  // Versions
  object V {
    val Slf4j        = "1.6.4"
    val Akka         = "2.0"
  }

  // Compile
  val slf4jLog4j    = "org.slf4j"         % "slf4j-log4j12"% V.Slf4j
  val akkaActor     = "com.typesafe.akka" % "akka-actor"   % V.Akka
  val playmini      = "com.typesafe" %% "play-mini" % "2.0-RC3"
  val akkaSlf4j     = "com.typesafe.akka" % "akka-slf4j"   % V.Akka

}
