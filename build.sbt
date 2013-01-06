import sbt.Defaults._

sbtPlugin := true

name := "play-plovr-plugin"

version := "0.3-SNAPSHOT"

organization := "com.benmccann"

scalacOptions += "-deprecation"

addSbtPlugin("play" % "sbt-plugin" % "2.1-RC1" % "provided")

libraryDependencies += "com.sun.jna" % "jna" % "3.0.9"

publishMavenStyle := false

publishTo := Some(Resolver.url("sbt-plugin-releases", new URL("http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases/"))(Resolver.ivyStylePatterns))
