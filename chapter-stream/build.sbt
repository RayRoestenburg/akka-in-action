enablePlugins(JavaServerAppPackaging)

name := "stream"

version := "1.0"

organization := "com.manning"

libraryDependencies ++= {
  val version = "2.4.9"
  Seq(
    "com.typesafe.akka" %% "akka-actor" % version,
    //<start id="stream-dependencies">
    "com.typesafe.akka" %% "akka-stream" % version, //<co id="stream-dep"/>
    //<end id="stream-dependencies">
    //<start id="stream-http-dependencies">
    "com.typesafe.akka" %% "akka-http-core"                    % version, //<co id="http-dep"/>
    "com.typesafe.akka" %% "akka-http-experimental"            % version, //<co id="http-dep"/>
    "com.typesafe.akka" %% "akka-http-spray-json-experimental" % version, //<co id="json-dep"/>
    //<end id="stream-http-dependencies">
    //<start id="test-dependencies">
    "com.typesafe.akka" %% "akka-stream-testkit" % version % "test", //<co id="stream-test-dep"/>
    "com.typesafe.akka" %% "akka-testkit"        % version % "test",
    "org.scalatest"     %% "scalatest"           % "2.2.6" % "test"
    //<end id="test-dependencies">
  )
}
