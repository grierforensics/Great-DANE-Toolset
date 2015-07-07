
name := "dst"

organization := "com.grierforensics"

version := "0.3"

scalaVersion := "2.11.4"

resolvers += Resolver.sonatypeRepo("releases")


lazy val root = (project in file(".")) aggregate(bc, core, web)

lazy val bc = (project in file("dst-bc"))

lazy val core = (project in file("dst-core"))
  .dependsOn(bc % "compile->compile;test->test")

lazy val web = (project in file("dst-web"))
  .dependsOn(core % "compile->compile;test->test")

