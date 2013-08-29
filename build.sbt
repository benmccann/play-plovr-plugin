import sbt.Defaults._

sbtPlugin := true

name := "play-plovr-plugin"

version := "0.4-SNAPSHOT"

organization := "com.benmccann"

scalacOptions += "-deprecation"

resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.2.0-M2" % "provided")

libraryDependencies += "com.sun.jna" % "jna" % "3.0.9"

publishMavenStyle := false

publishTo := Some(Resolver.url("sbt-plugin-releases", new URL("http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases/"))(Resolver.ivyStylePatterns))
