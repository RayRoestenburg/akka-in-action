name := "next"

version := "1.0"

organization := "com.manning"

resolvers ++= Seq("Typesafe Snapshots" at "http://repo.akka.io/snapshots/")

parallelExecution in Test := false

fork := true

libraryDependencies ++= {
  val akkaVersion = "2.4.7"
  Seq(
    "com.typesafe.akka"         %%  "akka-actor"              % akkaVersion,
    "com.typesafe.akka"         %%  "akka-typed-experimental" % akkaVersion,
    "com.typesafe.akka"         %%  "akka-persistence"        % akkaVersion,
    "commons-io"                %   "commons-io"              % "2.4",
    "org.scalatest"             %%  "scalatest"               % "2.2.4"      % "test"
  )
}
