name := "persistence"

version := "1.0"

organization := "com.manning"

scalaVersion := "2.11.6"

scalacOptions ++= Seq(
  "-deprecation",
  "-unchecked",
  "-Xlint",
  "-Ywarn-unused",
  "-Ywarn-dead-code",
  "-feature",
  "-language:_"
)

resolvers ++= Seq("Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
                  "Sonatype snapshots"  at "http://oss.sonatype.org/content/repositories/snapshots/")

parallelExecution in Test := false

fork := true

libraryDependencies ++= {
  val akkaVersion       = "2.3.12"
  val sprayVersion      = "1.3.3"
  Seq(
    "com.typesafe.akka"         %%  "akka-actor"                     % akkaVersion,
    "com.typesafe.akka"         %%  "akka-slf4j"                     % akkaVersion,
    "com.typesafe.akka"         %%  "akka-persistence-experimental"  % akkaVersion,
    "com.typesafe.akka"         %%  "akka-cluster"                   % akkaVersion,
    "com.typesafe.akka"         %%  "akka-contrib"                   % akkaVersion,
    "com.typesafe.akka"         %%  "akka-testkit"                   % akkaVersion   % "test",
    "com.typesafe.akka"         %%  "akka-multi-node-testkit"        % akkaVersion   % "test",
    "io.spray"                  %%  "spray-can"                      % sprayVersion,
    "io.spray"                  %%  "spray-client"                   % sprayVersion,
    "io.spray"                  %%  "spray-json"                     % "1.3.2",
    "io.spray"                  %%  "spray-routing"                  % sprayVersion,
    "commons-io"                %   "commons-io"                     % "2.4",
    "org.scalatest"             %%  "scalatest"                      % "2.2.4"       % "test",
    "ch.qos.logback"            %   "logback-classic"                % "1.1.2",
    "io.spray"                  %%  "spray-can"                      % sprayVersion,
    "io.spray"                  %%  "spray-routing"                  % sprayVersion
  )
}

// Assembly settings
mainClass in Global := Some("aia.persistence.sharded.ShardedMain")

jarName in assembly := "persistence-examples.jar"
