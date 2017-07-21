enablePlugins(JavaServerAppPackaging)

name := "stream"

version := "1.0"

organization := "com.manning"

libraryDependencies ++= {
  val akkaVersion = "2.4.19"
  val akkaHttpVersion = "10.0.9"
  Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    //<start id="stream-dependencies">
    "com.typesafe.akka" %% "akka-stream" % akkaVersion,
    //<end id="stream-dependencies">
    //<start id="stream-http-dependencies">
    "com.typesafe.akka" %% "akka-http-core"       % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-http"            % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
    //<end id="stream-http-dependencies">
    //<start id="test-dependencies">
    "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % "test",
    "com.typesafe.akka" %% "akka-testkit"        % akkaVersion % "test",
    "org.scalatest"     %% "scalatest"           % "3.0.0" % "test"
    //<end id="test-dependencies">
  )
}
