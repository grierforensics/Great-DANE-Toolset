name := "dst-web"

organization := "com.grierforensics"

version := "0.2"

scalaVersion := "2.11.4"

artifactName := { (sv: ScalaVersion, module: ModuleID, artifact: Artifact) =>
  artifact.name + "-" + module.revision + "." + artifact.extension
}

jetty()
//tomcat()

resolvers += Resolver.sonatypeRepo("releases")

libraryDependencies ++= Seq(
  "com.grierforensics" %% "dst-core" % "0.2",
  "javax.servlet" % "javax.servlet-api" % "3.1.0" % "provided",
  "org.glassfish.jersey.containers" % "jersey-container-servlet-core" % "2.14"
)

libraryDependencies ++= Seq( // test
  "org.eclipse.jetty" % "jetty-webapp" % "9.1.0.v20131115" % "test",
  "org.eclipse.jetty" % "jetty-plus" % "9.1.0.v20131115" % "test",
  "javax.servlet" % "javax.servlet-api" % "3.1.0" % "test",
  "org.scalatest" %% "scalatest" % "2.2.1" % "test"
)

webInfClasses in webapp := true