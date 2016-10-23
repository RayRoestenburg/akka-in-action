enablePlugins(JavaServerAppPackaging)

name := "stream"

version := "1.0"

organization := "com.manning"

libraryDependencies ++= {
  val version = "2.4.11"
  Seq(
    "com.typesafe.akka" %% "akka-actor" % version,
    //<start id="stream-dependencies">
    "com.typesafe.akka" %% "akka-stream" % version,
    //<end id="stream-dependencies">
    //<start id="stream-http-dependencies">
    "com.typesafe.akka" %% "akka-http-core"                    % version,
    "com.typesafe.akka" %% "akka-http-experimental"            % version,
    "com.typesafe.akka" %% "akka-http-spray-json-experimental" % version,
    //<end id="stream-http-dependencies">
    //<start id="test-dependencies">
    "com.typesafe.akka" %% "akka-stream-testkit" % version % "test",
    "com.typesafe.akka" %% "akka-testkit"        % version % "test",
    "org.scalatest"     %% "scalatest"           % "2.2.6" % "test"
    //<end id="test-dependencies">
  )
}
