package com.benmccann.playplovr

import sbt._

trait PlayPlovrKeys {

  val plovrTargets = SettingKey[Seq[(File,String)]]("plovr-targets", "Pairs of JSON configuration files used by plovr and corresponding target paths (relative to the project root) for compiled Javascript")
  val plovrEntryPoints = SettingKey[PathFinder]("plovr-entry-points", "The files that are compiled with plovr and are watched for changes")
  val plovrDir = SettingKey[File]("plovr-directory", "Directory where the plovr jar and configurations are placed")
  val plovrTmpDir = SettingKey[File]("plovr-tmp-dir", "Temporary directory where the plovr jar will be placed")

  val cleanJs = TaskKey[Unit]("plovr-clean", "Remove all compiled js files")
  val compileJs = TaskKey[Seq[File]]("plovr-compile", "Compile javascripts with plovr if needed")
  val startJsDaemon = TaskKey[Unit]("plovr-daemon-start", "Serve javascript files over http")
  val stopJsDaemon = TaskKey[Unit]("plovr-daemon-stop", "Stop serving javascript files over http")

}
