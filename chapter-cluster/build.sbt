name := "words-cluster"

version := "0.1-SNAPSHOT"

organization := "com.manning"

scalaVersion := "2.11.6"

resolvers ++= Seq("Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
                  "Sonatype snapshots"  at "http://oss.sonatype.org/content/repositories/snapshots/",
                  "Spray Repository"    at "http://repo.spray.io",
                  "Spray Nightlies"     at "http://nightlies.spray.io/")

libraryDependencies ++= {
  val akkaVersion       = "2.3.10"
  val sprayVersion      = "1.3.3"
  Seq(
    "com.typesafe.akka"       %%  "akka-actor"                     % akkaVersion,
    "com.typesafe.akka"       %%  "akka-slf4j"                     % akkaVersion,
    "com.typesafe.akka"       %%  "akka-remote"                    % akkaVersion,
    "com.typesafe.akka"       %%  "akka-cluster"                   % akkaVersion,
    "com.typesafe.akka"       %%  "akka-multi-node-testkit"        % akkaVersion   % "test",
    "com.typesafe.akka"       %%  "akka-testkit"                   % akkaVersion   % "test",
    "org.scalatest"           %% "scalatest"                       % "2.2.0"       % "test",
    "io.spray"                %% "spray-can"                       % sprayVersion,
    "io.spray"                %% "spray-routing"                   % sprayVersion,
    "io.spray"                %% "spray-json"                      % "1.3.1",
    "com.typesafe.akka"       %% "akka-slf4j"                      % akkaVersion,
    "ch.qos.logback"          %  "logback-classic"                 % "1.0.10"
  )
}

// Assembly settings
mainClass in Global := Some("aia.cluster.words.Main")

jarName in assembly := "words-node.jar"
