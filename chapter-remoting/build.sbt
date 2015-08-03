import com.typesafe.sbt.SbtStartScript

seq(SbtStartScript.startScriptForClassesSettings: _*)

name := "goticks"

version := "0.1-SNAPSHOT"

organization := "com.goticks"

scalaVersion := "2.11.7"

resolvers ++= Seq("Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
                  "Sonatype snapshots"  at "http://oss.sonatype.org/content/repositories/snapshots/",
                  "Spray Repository"    at "http://repo.spray.io",
                  "Spray Nightlies"     at "http://nightlies.spray.io/")

libraryDependencies ++= {
  val akkaVersion       = "2.3.12"
  val sprayVersion      = "1.3.3"
  Seq(
    "com.typesafe.akka" %%  "akka-actor"              % akkaVersion,
    "com.typesafe.akka" %%  "akka-slf4j"              % akkaVersion,
//<start id="remote_dependencies"/>
    "com.typesafe.akka" %%  "akka-remote"             % akkaVersion, //<co id="remote"/>
    "com.typesafe.akka" %%  "akka-multi-node-testkit" % akkaVersion % "test", //<co id="multinode"/>
//<end id="remote_dependencies"/>
    "com.typesafe.akka" %%  "akka-testkit"            % akkaVersion % "test",
    "org.scalatest"     %% "scalatest"                % "2.2.4"     % "test",
    "io.spray"          %% "spray-can"                % sprayVersion,
    "io.spray"          %% "spray-routing"            % sprayVersion,
    "io.spray"          %% "spray-json"                      % "1.3.1",
    "ch.qos.logback"    %  "logback-classic"                 % "1.1.2"
  )
}

// Assembly settings
mainClass in Global := Some("com.goticks.Main")

jarName in assembly := "goticks-server.jar"
