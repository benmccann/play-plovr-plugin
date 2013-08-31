package com.benmccann.playplovr

import sbt._
import Keys._
import collection.mutable.ArrayBuffer
import java.io.FileOutputStream
import java.net.URL
import java.nio.channels.{Channels, ReadableByteChannel}

/**
 * SBT settings for running plovr from inside the SBT/Play-console.
 *
 * @author Ben McCann (benmccann.com)
 * @author Johan Andren (johan.andren@mejsla.se)
 */
object PlayPlovrPlugin extends Plugin with PlayPlovrKeys {

  lazy val defaultPlovrSettings: Seq[Setting[_]] = Seq(
      plovrTmpDir := new File("/tmp"),
      cleanJsSetting,
      compileJsSetting,
      startJsDaemonSetting,
      stopJsDaemonSetting,

      plovrEntryPoints <<= (sourceDirectory in Compile)(base => ((base / "assets" ** "*.js") --- (base / "assets" ** "_*"))),

      // disable Play's built-in JavaScript compilation
      play.Project.javascriptEntryPoints := Seq(),

      // start the plovr daemon whenever compile is invoked
      compile in Compile <<= (compile in Compile).dependsOn(startJsDaemon),

      // do a compilation to disk when deploying to production
      // hook into buildRequire because it's the one thing that happens with start, stage, and dist
      play.Project.buildRequire <<= play.Project.buildRequire.dependsOn(compileJs)
    )

  lazy val cleanJsSetting: Setting[Task[Unit]] = cleanJs <<= (plovrTargetPath, classDirectory in Compile) map {
    case (target: String, classes: File) => {
      IO.delete(new File(classes, target))
    }
  }

  lazy val compileJsSetting: Setting[Task[Seq[File]]] = compileJs <<= compileJsTask
  lazy val compileJsTask = (plovrConfiguration in compileJs, plovrTmpDir, plovrTargetPath, plovrEntryPoints, classDirectory in Compile, streams) map {
    case (configFile: File, plovrTmpDir: File, target: String, jsEntryPoints: PathFinder, classes: File, s: TaskStreams) => {

      val jsFiles = jsEntryPoints.get
      val targetFile = new File(classes, target)
      val gzTarget = new File(targetFile.getAbsolutePath + ".gz")

      val newest = jsFiles.maxBy(_.lastModified)
      s.log.debug("Newest JS change: " + newest.lastModified + ": " + newest)
      s.log.debug("Existing JS timestamp:      " + targetFile.lastModified + ": " + targetFile)

      if (!targetFile.getParentFile.exists) {
        targetFile.getParentFile.mkdirs()
      }

      if (!targetFile.exists || (targetFile.exists && targetFile.lastModified <= newest.lastModified)) {
        IO.delete(targetFile)

        s.log.debug("Using plovr configuration file '" + configFile + "'")
        s.log.info("Compiling " + jsFiles.size + " Javascript sources to " + targetFile.getAbsolutePath)

        plovrCompile(plovrTmpDir, targetFile, configFile, s) match {

          // failure
          case Left(output) => {
            throw new RuntimeException(output.reverse.mkString("\n"))
          }

          // success
          case Right(_) => {
            IO.gzip(targetFile, gzTarget)
            s.log.info("Javascript compiled, total size: " + targetFile.length / 1000 + " k, gz: " + gzTarget.length / 1000 + " k")
          }
        }

      }

      if (targetFile.length == 0) {
        IO.delete(targetFile)
        throw new RuntimeException("JavaScript compilation failed")
      }

      Seq(targetFile, gzTarget)
    }
  }

  private def plovrCompile(plovrTmpDir: File, targetFile: File, configFile: File, s: TaskStreams): Either[Seq[String], Seq[String]] = {
    targetFile.createNewFile()
    val plovrJar: File = ensurePlovrJar(plovrTmpDir, s)
    val command = "java -jar " + plovrJar.getAbsolutePath + " build " + configFile.getAbsolutePath
    s.log.debug("Plovr compile command: " + command)

    var success = true;
    var compilerOutput = ArrayBuffer[String]()
    object outputLogger extends sbt.ProcessLogger {
      // stdout is piped to file
      def info(message: => String) { output(message) }

      // this is the actual output from the compiler
      def error(message: => String) { output(message) }

      def output(message: String) {
        if (message.startsWith("BUILD FAILED")) {
          success = false;
        }

        if (!outputShouldBeFilteredOut(message)) {
          compilerOutput.append(message)
        }
      }

      def buffer[T](f: => T) = f
    }

    command #> targetFile ! outputLogger

    if (success) {
      Right(compilerOutput.toSeq)
    } else {
      IO.delete(targetFile)
      Left(compilerOutput.toSeq)
    }
  }

  /** keep track of the plovr daemon process so that it can be stopped later */
  var plovrProcess: Option[Process] = None

  private def outputShouldBeFilteredOut(message: String) =
    message.isEmpty ||
      message.contains("org.plovr.Manifest") ||
      message.contains(".DS_Store")

  lazy val startJsDaemonSetting: Setting[Task[Unit]] = startJsDaemon <<= (plovrConfiguration in startJsDaemon, plovrTmpDir, streams) map {
    case (configFile: File, plovrTmpDir: File, s: TaskStreams) => {

      // check if daemon is running already
      import java.net.Socket
      val alreadyRunning =
        try {
          val socket = new Socket("127.0.0.1", 9810);
          socket.close();
          true
        } catch {
          case _ : Throwable => false
        }


      if (!alreadyRunning) {

        s.log.info("Starting plovr daemon serving '" + configFile + "' at http://localhost:9810")
        val plovrJar: File = ensurePlovrJar(plovrTmpDir, s)
        val command = "java -jar " + plovrJar.getAbsolutePath + " serve " + configFile.getAbsolutePath

        val daemonOutputLogger = new sbt.ProcessLogger {
          def error(message: => String) {
            // plovr outputs its logs to stderr
            if (!outputShouldBeFilteredOut(message))
              s.log.info(message)
          }

          def info(message: => String) {
            if (!outputShouldBeFilteredOut(message))
              s.log.info(message)
          }

          def buffer[T](f: => T) = f
        }

        plovrProcess = Some(Process(command).run(daemonOutputLogger))
      }
    }
  }

  lazy val stopJsDaemonSetting: Setting[Task[Unit]] = stopJsDaemon <<= streams map {
    case (s: TaskStreams) => {
      plovrProcess match {
        case Some(process: Process) => {
          s.log.info("Shutting down plovr daemon")
          process.destroy()
          plovrProcess = None
        }
        case None => s.log.info("Plovr daemon not running")
      }
    }
  }

  private def ensurePlovrJar(plovrTmpDir: File, s: TaskStreams): File = {
    val plovrRelease = "plovr-81ed862.jar"
    val plovrJar: File = new File(plovrTmpDir, plovrRelease)
    if (plovrJar.exists()) {
      return plovrJar;
    }
    val url: URL = new URL("http://plovr.googlecode.com/files/" + plovrRelease);
    val rbc: ReadableByteChannel = Channels.newChannel(url.openStream());
    val fos: FileOutputStream = new FileOutputStream(plovrJar);
    fos.getChannel().transferFrom(rbc, 0, java.lang.Long.MAX_VALUE);
    s.log.info("Downloaded " + url + " to " + plovrJar);
    plovrJar
  }

}
