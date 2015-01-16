name := "dst"

organization := "com.grierforensics"

version := "0.1.0-SNAPSHOT"

scalaVersion := "2.11.4"

jetty()
//tomcat()

resolvers += Resolver.sonatypeRepo("releases")

val jacksonVersion = "2.4.4"
val jerseyVersion = "2.14"

libraryDependencies ++= Seq(
  "javax.servlet" % "javax.servlet-api" % "3.1.0" % "provided",
  "com.fasterxml.jackson.core" % "jackson-annotations" % jacksonVersion,
  "com.fasterxml.jackson.core" % "jackson-databind" % jacksonVersion,
  "com.fasterxml.jackson.jaxrs" % "jackson-jaxrs-json-provider" % jacksonVersion,
  "com.fasterxml.jackson.jaxrs" % "jackson-jaxrs-base" % jacksonVersion,
  "com.fasterxml.jackson.module" % "jackson-module-scala_2.11" % jacksonVersion,
  "org.glassfish.jersey.containers" % "jersey-container-servlet-core" % jerseyVersion,
  "com.owlike" % "genson" % "1.2"
)

libraryDependencies ++= Seq( // test
  "org.eclipse.jetty" % "jetty-webapp" % "9.1.0.v20131115" % "test",
  "org.eclipse.jetty" % "jetty-plus" % "9.1.0.v20131115" % "test",
  "javax.servlet" % "javax.servlet-api" % "3.1.0" % "test",
  "org.scalatest" %% "scalatest" % "2.2.1" % "test"
)