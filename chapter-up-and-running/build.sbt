enablePlugins(JavaServerAppPackaging) //<co id="example-enable-sbt-native-packager"/>

name := "goticks"

version := "1.0"

organization := "com.goticks" //<co id="example-app-info"/>

scalaVersion := "2.11.7"

resolvers ++=
  Seq(Resolver.typesafeRepo("releases"),
      "Spray Repository"    at "http://repo.spray.io") //<co id="example-resolvers"/>

libraryDependencies ++= {
  val akkaVersion       = "2.4.2-RC2" //<co id="akkaVersion"/>
  Seq(
    "com.typesafe.akka" %% "akka-actor"      % akkaVersion, //<co id="actorDep"/>
    "com.typesafe.akka" %% "akka-http-core"  % akkaVersion, 
    "com.typesafe.akka" %% "akka-http-experimental"  % akkaVersion, 
    "com.typesafe.akka" %% "akka-http-spray-json-experimental"  % akkaVersion, 
    "io.spray"          %% "spray-json"      % "1.3.1",
    "com.typesafe.akka" %% "akka-slf4j"      % akkaVersion,
    "ch.qos.logback"    %  "logback-classic" % "1.1.3",
    "com.typesafe.akka" %% "akka-testkit"    % akkaVersion   % "test",
    "org.scalatest"     %% "scalatest"       % "2.2.0"       % "test"
  )
}
