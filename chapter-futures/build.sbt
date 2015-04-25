name := "futures"

version := "0.1-SNAPSHOT"

organization := "com.goticks"

scalaVersion := "2.11.6"

resolvers ++=
  Seq(Resolver.typesafeRepo("releases"),
      "Spray Repository"    at "http://repo.spray.io")

libraryDependencies ++= {
  val akkaVersion       = "2.3.10"
  val sprayVersion      = "1.3.3"
  Seq(
    "com.typesafe.akka"       %%  "akka-actor"                     % akkaVersion,
    "com.typesafe.akka"       %%  "akka-slf4j"                     % akkaVersion,
    "com.typesafe.akka"       %%  "akka-testkit"                   % akkaVersion   % "test",
    "org.scalatest"           %% "scalatest"                       % "2.2.4"       % "test",
    "io.spray"                %%  "spray-can"                      % sprayVersion,
    "io.spray"                %%  "spray-routing"                  % sprayVersion,
    "io.spray"                %% "spray-json"                      % "1.3.1",
    "com.typesafe.akka"       %% "akka-slf4j"                      % akkaVersion,
    "ch.qos.logback"          %  "logback-classic"                 % "1.1.3",
    "com.github.nscala-time"  %%  "nscala-time"                    % "1.8.0"
  )
}
