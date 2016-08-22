enablePlugins(JavaServerAppPackaging) //<co id="example-enable-sbt-native-packager"/>

name := "goticks"

version := "1.0"

organization := "com.goticks" //<co id="example-app-info"/>

libraryDependencies ++= {
  val akkaVersion = "2.4.9" //<co id="akkaVersion"/>
  Seq(
    "com.typesafe.akka" %% "akka-actor"      % akkaVersion, //<co id="actorDep"/>
    "com.typesafe.akka" %% "akka-http-core"  % akkaVersion, 
    "com.typesafe.akka" %% "akka-http-experimental"  % akkaVersion, 
    "com.typesafe.akka" %% "akka-http-spray-json-experimental"  % akkaVersion, 
    "com.typesafe.akka" %% "akka-slf4j"      % akkaVersion,
    "ch.qos.logback"    %  "logback-classic" % "1.1.3",
    "com.typesafe.akka" %% "akka-testkit"    % akkaVersion   % "test",
    "org.scalatest"     %% "scalatest"       % "2.2.0"       % "test"
  )
}
