package com.benmccann.playplovr

import sbt._

trait PlayPlovrKeys {

  val plovrConfiguration = SettingKey[File]("plovr-configuration", "JSON-configuration file to use with plovr")
  val plovrEntryPoints = SettingKey[PathFinder]("plovr-entry-points", "The files that are compiled with plovr and are watched for changes")
  val plovrDir = SettingKey[File]("plovr-directory", "Directory where the plovr jar and configurations are placed")
  val plovrTmpDir = SettingKey[File]("plovr-tmp-dir", "Temporary directory where the plovr jar will be placed")
  val plovrDaemonLog = SettingKey[File]("plovr-daemon-log", "Path to the plovr logfile")
  val plovrTargetFile = SettingKey[File]("plovr-target", "File to compile js into, should be relative to the resource-managed setting")

  val cleanJs = TaskKey[Unit]("plovr-clean", "Remove all compiled js files")
  val compileJs = TaskKey[Seq[File]]("plovr-compile", "Compile javascripts with plovr if needed")
  val startJsDaemon = TaskKey[Unit]("plovr-daemon-start", "Serve javascript files over http")
  val stopJsDaemon = TaskKey[Unit]("plovr-daemon-stop", "Stop serving javascript files over http")

}
