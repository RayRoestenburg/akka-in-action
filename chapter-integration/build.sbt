name := "integration"

version := "0.1-SNAPSHOT"

organization := "manning"

scalaVersion := "2.11.7"

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
  val akkaVersion     = "2.3.12"
  val sprayVersion    = "1.3.3"
  val camelVersion    = "2.13.2"
  val activeMQVersion = "5.4.1"
  Seq(
    "org.scala-lang.modules"  %% "scala-xml" 				 % "1.0.2",
    "com.typesafe.akka"       %% "akka-camel"        % akkaVersion,
    "net.liftweb"             %  "lift-json_2.10"    % "3.0-M1",
    "io.spray"                %% "spray-can"         % sprayVersion,
    "io.spray"                %% "spray-routing"     % sprayVersion,
    "com.typesafe.akka"       %% "akka-actor"        % akkaVersion,
    "com.typesafe.akka"       %% "akka-slf4j"        % akkaVersion,
    "commons-io"              %  "commons-io"        % "2.0.1"		     % "test",
    "io.spray"                %% "spray-testkit"     % "1.3.3"         % "test",
    "org.apache.camel"        %  "camel-mina"        % camelVersion 	 % "test",
    "org.apache.activemq"     %  "activemq-camel"    % activeMQVersion % "test",
    "org.apache.activemq"     %  "activemq-core"     % activeMQVersion % "test",
    "org.apache.camel"        %  "camel-jetty"       % camelVersion 	 % "test",
    "com.typesafe.akka"       %% "akka-testkit"      % akkaVersion     % "test",
    "org.scalatest"           %% "scalatest"         % "2.2.4"         % "test"
  )
}

