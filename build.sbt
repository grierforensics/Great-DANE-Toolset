
name := "dst"

organization := "com.grierforensics"

version := "0.1.0-SNAPSHOT"

scalaVersion := "2.11.4"

resolvers += Resolver.sonatypeRepo("releases")


lazy val root = (project in file(".")) aggregate(bc, core, web)

lazy val bc = (project in file("dst-bc"))

lazy val core = (project in file("dst-core"))
  .dependsOn(bc)

lazy val web = (project in file("dst-web"))
  .dependsOn(core)
