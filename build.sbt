import xerial.sbt.Pack._

name := "tjmx"

scalaVersion := "2.10.1"

resolvers += "JAnalyse Repository" at "http://www.janalyse.fr/repository/"

resolvers += "sonatype-public" at "https://oss.sonatype.org/content/groups/public"

libraryDependencies += "fr.janalyse" %% "janalyse-jmx" % "0.6.3" % "compile"

libraryDependencies += "com.github.scopt" %% "scopt" % "3.0.0"

unmanagedClasspath  in Compile += Attributed.blank(file("/usr/lib/jvm/jdk1.7.0/lib/tools.jar"))

unmanagedClasspath  in Runtime += Attributed.blank(file("/usr/lib/jvm/jdk1.7.0/lib/tools.jar"))

//When run without forking, sbt tries to load tools.jar from multiple class loaders. tools.jar in turn tries to pull in native libs. This causes errors.
fork in Compile := true

seq(packSettings :_*)

packMain := Map("tjmx" -> "tjmx.TJmx")

packJvmOpts := Map("tjmx" -> Seq("-Xms8M"))
