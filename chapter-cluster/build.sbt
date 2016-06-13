name := "words-cluster"

version := "1.0"

organization := "com.manning"

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

libraryDependencies ++= {
  val akkaVersion = "2.3.12"
  val sprayVersion      = "1.3.3"
  Seq(
    "com.typesafe.akka"       %%  "akka-actor"                     % akkaVersion,
    "com.typesafe.akka"       %%  "akka-slf4j"                     % akkaVersion,
    "com.typesafe.akka"       %%  "akka-remote"                    % akkaVersion,
    "com.typesafe.akka"       %%  "akka-cluster"                   % akkaVersion,
    "com.typesafe.akka"       %%  "akka-multi-node-testkit"        % akkaVersion   % "test",
    "com.typesafe.akka"       %%  "akka-testkit"                   % akkaVersion   % "test",
    "org.scalatest"           %% "scalatest"                       % "2.2.4"       % "test",
    "io.spray"                %% "spray-can"                       % sprayVersion,
    "io.spray"                %% "spray-routing"                   % sprayVersion,
    "io.spray"                %% "spray-json"                      % "1.3.2",
    "com.typesafe.akka"       %% "akka-slf4j"                      % akkaVersion,
    "ch.qos.logback"          %  "logback-classic"                 % "1.0.10"
  )
}

// Assembly settings
mainClass in Global := Some("aia.cluster.words.Main")

jarName in assembly := "words-node.jar"
