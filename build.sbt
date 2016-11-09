name := "all"

version := "1.0"

organization := "com.manning"

lazy val channels    = project.in(file("chapter-channels"))

lazy val cluster     = project.in(file("chapter-cluster"))

lazy val conf        = project.in(file("chapter-conf-deploy"))

lazy val fault       = project.in(file("chapter-fault-tolerance"))

lazy val futures     = project.in(file("chapter-futures"))

lazy val integration = project.in(file("chapter-integration"))

lazy val looking     = project.in(file("chapter-looking-ahead"))

lazy val persistence = project.in(file("chapter-persistence"))

lazy val remoting    = project.in(file("chapter-remoting"))

lazy val routing     = project.in(file("chapter-routing"))

lazy val state       = project.in(file("chapter-state"))

lazy val stream      = project.in(file("chapter-stream"))

lazy val structure   = project.in(file("chapter-structure"))

lazy val test        = project.in(file("chapter-testdriven"))

lazy val up          = project.in(file("chapter-up-and-running"))
