resolvers += Classpaths.typesafeResolver

resolvers += "sbt-idea" at "http://mpeltonen.github.com/maven/"

addSbtPlugin("com.github.mpeltonen" % "sbt-idea" % "1.4.0")

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.8.7")

addSbtPlugin("com.typesafe.startscript" % "xsbt-start-script-plugin" % "0.5.3")

addSbtPlugin("com.typesafe.sbt" % "sbt-multi-jvm" % "0.3.4")
