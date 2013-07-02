name := "tjmx"

scalaVersion := "2.10.1"

resolvers += "JAnalyse Repository" at "http://www.janalyse.fr/repository/"

resolvers += "sonatype-public" at "https://oss.sonatype.org/content/groups/public"

libraryDependencies += "fr.janalyse" %% "janalyse-jmx" % "0.6.3" % "compile"

libraryDependencies += "com.github.scopt" %% "scopt" % "3.0.0"

unmanagedClasspath  in Compile += Attributed.blank(file("/usr/lib/jvm/jdk1.7.0/lib/tools.jar"))

unmanagedClasspath  in Runtime += Attributed.blank(file("/usr/lib/jvm/jdk1.7.0/lib/tools.jar"))

fork in Runtime := true
