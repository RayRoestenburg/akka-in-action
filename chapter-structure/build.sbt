name := "structure"

version := "1.0"

organization := "com.manning"

scalacOptions ++= Seq(
  "-deprecation",
  "-unchecked",
  "-Xlint",
  "-Ywarn-unused",
  "-Ywarn-dead-code",
  "-feature",
  "-language:_"
)

libraryDependencies ++= {
  val akkaVersion = "2.4.19"
  Seq(
    "com.typesafe.akka" %%  "akka-actor"   % akkaVersion,
    "com.typesafe.akka" %%  "akka-slf4j"   % akkaVersion,
    "com.typesafe.akka" %%  "akka-testkit" % akkaVersion  % "test",
    "org.scalatest"     %%  "scalatest"    % "3.0.0"      % "test"
  )
}
