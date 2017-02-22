// Copyright (C) 2017 Grier Forensics. All Rights Reserved.

name := "dst-web"

organization := "com.grierforensics"

version := "1.0"

scalaVersion := "2.11.8"

artifactName := { (sv: ScalaVersion, module: ModuleID, artifact: Artifact) =>
  artifact.name + "-" + module.revision + "." + artifact.extension
}

resolvers += Resolver.sonatypeRepo("releases")

libraryDependencies ++= Seq(
  "com.grierforensics" %% "dst-core" % "1.0",
  "javax.servlet" % "javax.servlet-api" % "3.1.0" % "provided",
  "org.glassfish.jersey.containers" % "jersey-container-servlet-core" % "2.14"
)

libraryDependencies ++= Seq(
  "org.eclipse.jetty" % "jetty-webapp" % "9.1.0.v20131115" % "test",
  "org.eclipse.jetty" % "jetty-plus" % "9.1.0.v20131115" % "test",
  "javax.servlet" % "javax.servlet-api" % "3.1.0" % "test",
  "org.scalatest" %% "scalatest" % "3.0.1" % "test"
)

enablePlugins(TomcatPlugin)
