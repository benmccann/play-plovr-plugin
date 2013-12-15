import sbt.Defaults._

sbtPlugin := true

name := "play-plovr-plugin"

version := "0.4"

organization := "com.benmccann"

scalacOptions += "-deprecation"

resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.2.1" % "provided")

libraryDependencies += "com.sun.jna" % "jna" % "3.0.9"

publishTo := Some(Resolver.url("sbt-plugin-releases", new URL("http://repo.scala-sbt.org/scalasbt/sbt-plugin-releases/"))(Resolver.ivyStylePatterns))

publishMavenStyle := false
