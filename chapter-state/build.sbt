name := "state"

version := "1.0"

organization := "com.manning"

libraryDependencies ++= {
  val akkaVersion = "2.4.9"
  Seq(
    "com.typesafe.akka" %%  "akka-actor"   % akkaVersion,
    "com.typesafe.akka" %%  "akka-agent"   % akkaVersion,
    "com.typesafe.akka" %%  "akka-slf4j"   % akkaVersion,
    "com.typesafe.akka" %%  "akka-testkit" % akkaVersion  % "test",
    "org.scalatest"     %%  "scalatest"    % "2.2.6"      % "test"
  )
}
