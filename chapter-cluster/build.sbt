name := "words-cluster"

version := "1.0"

organization := "com.manning"

libraryDependencies ++= {
  val akkaVersion = "2.4.9"
  Seq(
    "com.typesafe.akka"       %% "akka-actor"                        % akkaVersion,
    "com.typesafe.akka"       %% "akka-slf4j"                        % akkaVersion,
    "com.typesafe.akka"       %% "akka-remote"                       % akkaVersion,
    "com.typesafe.akka"       %% "akka-cluster"                      % akkaVersion,
    "com.typesafe.akka"       %% "akka-multi-node-testkit"           % akkaVersion   % "test",
    "com.typesafe.akka"       %% "akka-testkit"                      % akkaVersion   % "test",
    "org.scalatest"           %% "scalatest"                         % "2.2.6"       % "test",
    "com.typesafe.akka"       %% "akka-slf4j"                        % akkaVersion,
    "ch.qos.logback"          %  "logback-classic"                   % "1.0.10"
  )
}

// Assembly settings
mainClass in Global := Some("aia.cluster.words.Main")

jarName in assembly := "words-node.jar"
