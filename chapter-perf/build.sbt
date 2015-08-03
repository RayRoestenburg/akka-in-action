name := "performance"

version := "0.1-SNAPSHOT"

organization := "com.manning"

scalaVersion := "2.11.7"

libraryDependencies ++= {
  val akkaVersion       = "2.3.12"
  Seq(
    "com.typesafe.akka" %%  "akka-actor"   % akkaVersion,
    "com.typesafe.akka" %%  "akka-slf4j"   % akkaVersion,
    "com.typesafe.akka" %%  "akka-testkit" % akkaVersion   % "test",
    "org.scalatest"     %% "scalatest"     % "2.2.0"       % "test"
  )
}
