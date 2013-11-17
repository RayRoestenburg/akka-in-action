import AssemblyKeys._

name := "words-cluster"

version := "0.1-SNAPSHOT"

organization := "com.manning.aa"

scalaVersion := "2.10.0"

resolvers ++= Seq("Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
                  "Sonatype snapshots"  at "http://oss.sonatype.org/content/repositories/snapshots/",
                  "Spray Repository"    at "http://repo.spray.io",
                  "Spray Nightlies"     at "http://nightlies.spray.io/")

libraryDependencies ++= {
  val akkaVersion       = "2.3-M1"
  val sprayVersion      = "1.2-RC3"
  Seq(
    "com.typesafe.akka"       %%  "akka-actor"                     % akkaVersion,
    "com.typesafe.akka"       %%  "akka-slf4j"                     % akkaVersion,
    "com.typesafe.akka"       %%  "akka-remote"                    % akkaVersion,
    "com.typesafe.akka"       %%  "akka-cluster"                   % akkaVersion,
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
mainClass in Global := Some("com.manning.aa.words.Main")

jarName in assembly := "words-node.jar"

assemblySettings
