name := "persistence"

version := "1.0"

organization := "com.manning"

scalaVersion := "2.11.5"

resolvers ++= Seq("Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
                  "Sonatype snapshots"  at "http://oss.sonatype.org/content/repositories/snapshots/")

libraryDependencies ++= {
  val akkaVersion       = "2.3.9"
  Seq(
    "com.typesafe.akka"       %%  "akka-actor"                     % akkaVersion,
    "com.typesafe.akka"       %%  "akka-slf4j"                     % akkaVersion,
    "com.typesafe.akka"       %%  "akka-persistence-experimental"  % akkaVersion exclude("org.iq80.leveldb", "leveldb"),
    "org.iq80.leveldb"        %   "leveldb"                        % "0.7",
    "com.typesafe.akka"       %%  "akka-testkit"                   % akkaVersion   % "test",
    "commons-io"              %   "commons-io"                     % "2.4",
    "org.scalatest"           %%  "scalatest"                      % "2.2.4"       % "test",
    "ch.qos.logback"          %   "logback-classic"                % "1.1.2"
  )
}

// Assembly settings
mainClass in Global := Some("aia.persistence.Main")

jarName in assembly := "calculator.jar"
