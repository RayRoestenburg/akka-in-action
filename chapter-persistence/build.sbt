import com.typesafe.sbt.SbtStartScript
import scalariform.formatter.preferences._

name := "chat"

organization := "manning"

version := "0.1-SNAPSHOT"

scalaVersion := "2.11.1"

fork := true

scalacOptions := Seq("-encoding", "utf8",
                     "-target:jvm-1.7",
                     "-feature",
                     "-language:implicitConversions",
                     "-language:postfixOps",
                     "-unchecked",
                     "-deprecation",
                     "-Xlog-reflective-calls"
                    )

scalacOptions in Test ++= Seq("-Yrangepos")

mainClass := Some("com.manning.chat.Main")

resolvers ++= Seq("Sonatype Releases"   at "http://oss.sonatype.org/content/repositories/releases",
                  "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/")

libraryDependencies ++= {
  val akkaVersion  = "2.3.3"

  Seq(
    "com.typesafe.akka"       %% "akka-actor"                     % akkaVersion,
    "com.typesafe.akka"       %% "akka-persistence-experimental"  % akkaVersion,
    "com.typesafe.akka"       %% "akka-slf4j"                     % akkaVersion,
    "com.typesafe.akka"       %% "akka-testkit"                   % akkaVersion    % "test",
    "org.scalatest"           %  "scalatest_2.11"                 % "2.2.0"        % "test",
    "commons-io"              %  "commons-io"                     % "2.4"          % "test"
  )
}

seq(SbtStartScript.startScriptForClassesSettings: _*)

seq(Revolver.settings: _*)

scalariformSettings

ScalariformKeys.preferences := ScalariformKeys.preferences.value
  .setPreference(AlignSingleLineCaseStatements, true)
  .setPreference(PreserveDanglingCloseParenthesis, true)
  .setPreference(RewriteArrowSymbols, true)
  .setPreference(AlignParameters, false)
  .setPreference(DoubleIndentClassDeclaration, true)
  .setPreference(AlignSingleLineCaseStatements.MaxArrowIndent, 90)
