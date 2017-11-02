name := "persistence"

version := "1.0"

organization := "com.manning"

resolvers ++= Seq("Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
                  "Sonatype snapshots"  at "http://oss.sonatype.org/content/repositories/snapshots/")

parallelExecution in Test := false

fork := true

libraryDependencies ++= {
  val akkaVersion = "2.5.4"
  val akkaHttpVersion = "10.0.10"
  Seq(
    "com.typesafe.akka"         %%  "akka-actor"                          % akkaVersion,
    "com.typesafe.akka"         %%  "akka-stream"                         % akkaVersion,

    "com.typesafe.akka"         %%  "akka-persistence"                    % akkaVersion,
    "com.typesafe.akka"         %%  "akka-persistence-query"              % akkaVersion,
    "org.iq80.leveldb"           %  "leveldb"                             % "0.7",
    "org.fusesource.leveldbjni"  %  "leveldbjni-all"                      % "1.8",

    "com.typesafe.akka"         %%  "akka-cluster"                        % akkaVersion,
    "com.typesafe.akka"         %%  "akka-cluster-tools"                  % akkaVersion,
    "com.typesafe.akka"         %%  "akka-cluster-sharding"               % akkaVersion,

    "com.typesafe.akka"         %% "akka-http-core"                       % akkaHttpVersion,
    "com.typesafe.akka"         %% "akka-http"                            % akkaHttpVersion,
    "com.typesafe.akka"         %% "akka-http-spray-json"                 % akkaHttpVersion,

    "com.typesafe.akka"         %%  "akka-testkit"                        % akkaVersion   % "test",
    "com.typesafe.akka"         %%  "akka-multi-node-testkit"             % akkaVersion   % "test",

    "commons-io"                %   "commons-io"                          % "2.4",

    "org.scalatest"             %%  "scalatest"                           % "3.0.0"       % "test",

    "com.typesafe.akka"         %%  "akka-slf4j"                          % akkaVersion,
    "ch.qos.logback"            %   "logback-classic"                     % "1.1.2"
  )
}

// Assembly settings
mainClass in Global := Some("aia.persistence.sharded.ShardedMain")

assemblyJarName in assembly := "persistence-examples.jar"
