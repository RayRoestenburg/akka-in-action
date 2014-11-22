resolvers += Classpaths.typesafeResolver

resolvers += "sbt-idea" at "http://mpeltonen.github.com/maven/"

addSbtPlugin("com.github.mpeltonen" % "sbt-idea" % "1.6.0")

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.11.2")

addSbtPlugin("com.typesafe.sbt" % "sbt-start-script" % "0.10.0")

addSbtPlugin("com.typesafe.sbt" % "sbt-multi-jvm" % "0.3.8")
