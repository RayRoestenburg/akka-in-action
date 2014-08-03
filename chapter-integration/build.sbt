name := "integration"

version := "0.1-SNAPSHOT"

organization := "manning"

scalaVersion := "2.11.1"

libraryDependencies ++= {
  val akkaVersion       = "2.3.4"
  val sprayVersion      = "1.3.1"
  val camelVersion      = "2.13.2"
  val activeMQVersion   = "5.4.1"
  Seq(
    "org.scala-lang.modules"  %% "scala-xml" 				 % "1.0.2",
    "com.typesafe.akka"       %% "akka-camel"                      % akkaVersion,       //exclude ("org.slf4j", "slf4j-api") //ApacheV2
    "net.liftweb"             % "lift-json_2.10"                       % "3.0-M1",
    "io.spray"                %% "spray-can"                       % sprayVersion,
    "io.spray"                %% "spray-routing"                   % sprayVersion,
    "com.typesafe.akka"       %%  "akka-actor"                     % akkaVersion,
    "com.typesafe.akka"       %%  "akka-slf4j"                     % akkaVersion,
    "commons-io"               % "commons-io"                   	 % "2.0.1"		% "test",
    "io.spray"                    %  "spray-testkit"              % sprayVersion    % "test"  exclude ("com.typesafe.akka","akka-actor_2.10") exclude ("com.typesafe.akka","akka-testkit_2.10") exclude("io.spray","spray-util"),
    "org.apache.camel"            %  "camel-mina"                 % camelVersion 	% "test",
    "org.apache.activemq"         %  "activemq-camel"             % activeMQVersion % "test",
    "org.apache.activemq"         %  "activemq-core"              % activeMQVersion % "test",
    "org.apache.camel"            %  "camel-jetty"                % camelVersion 	% "test",
    "com.typesafe.akka"       %%  "akka-testkit"                   % akkaVersion   % "test",
    "org.scalatest"           %% "scalatest"                       % "2.2.0"       % "test"
//    "org.scala-lang.modules"  %% "scala-reflect" 				 % "1.0.1"
//    "org.scala-lang"              %  "scala-reflect"              % "2.11.1"      % "test",
  )
}


