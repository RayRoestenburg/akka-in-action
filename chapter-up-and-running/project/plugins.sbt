resolvers += Classpaths.typesafeReleases

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.13.0")

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.0.0")

// this plugin causes an eviction warning of sbt-assembly 0.12.0 -> 0.13.0 which can be safely ignored.
addSbtPlugin("com.typesafe.sbt" % "sbt-multi-jvm" % "0.3.9")
