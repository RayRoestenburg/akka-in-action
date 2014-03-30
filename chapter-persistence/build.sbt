import AssemblyKeys._

import scalariform.formatter.preferences._

name := "tickets-persistence"

version := "0.1-SNAPSHOT"

organization := "com.manning.tickets"

scalaVersion := "2.10.3"

resolvers ++= Seq("Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
                  "Sonatype snapshots"  at "http://oss.sonatype.org/content/repositories/snapshots/")

libraryDependencies ++= {
  val akkaVersion       = "2.3.0"
  Seq(
    "com.typesafe.akka"       %% "akka-actor"                     % akkaVersion,
    "com.typesafe.akka"       %% "akka-slf4j"                     % akkaVersion,
    "com.typesafe.akka"       %% "akka-persistence-experimental"  % akkaVersion,
    "com.typesafe.akka"       %% "akka-testkit"                   % akkaVersion   % "test",
    "org.scalatest"           %% "scalatest"                      % "2.1.0"       % "test",
    "com.typesafe.akka"       %% "akka-slf4j"                     % akkaVersion,
    "commons-io"              %  "commons-io"                     % "2.4"         % "test",
    "ch.qos.logback"          %  "logback-classic"                % "1.0.10"
  )
}

// Assembly settings
mainClass in Global := Some("com.manning.tickets.Main")

jarName in assembly := "tickets-persistence.jar"

assemblySettings

scalariformSettings

ScalariformKeys.preferences := ScalariformKeys.preferences.value
  .setPreference(AlignSingleLineCaseStatements, true)
  .setPreference(DoubleIndentClassDeclaration, false)
  .setPreference(PreserveDanglingCloseParenthesis, true)
  .setPreference(RewriteArrowSymbols, true)

fork := true
