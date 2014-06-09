package com.benmccann.playplovr

import sbt._
import Keys._
import collection.mutable.ArrayBuffer
import com.typesafe.sbt.web.SbtWeb
import com.typesafe.sbt.web.SbtWeb.autoImport._
import com.typesafe.sbt.web.pipeline.Pipeline
import java.io.FileOutputStream
import java.net.URL
import java.nio.channels.{Channels, ReadableByteChannel}

object Import {

  object PlovrKeys {
    val plovrTargets = SettingKey[Seq[(File,String)]]("plovr-targets", "Pairs of JSON configuration files used by plovr and corresponding target paths (relative to the project root) for compiled Javascript")
    val plovrEntryPoints = SettingKey[PathFinder]("plovr-entry-points", "The files that are compiled with plovr and are watched for changes")
    val plovrTmpDir = SettingKey[File]("plovr-tmp-dir", "Temporary directory where the plovr jar will be placed")

    val plovr = TaskKey[Seq[File]]("plovr", "Compile javascript with plovr if needed")
  }

}

/**
 * SBT settings for running plovr from inside the SBT/Play-console.
 *
 * @author Ben McCann (benmccann.com)
 * @author Johan Andren (johan.andren@mejsla.se)
 */
object PlayPlovrPlugin extends AutoPlugin {

  override def requires = SbtWeb

  override def trigger = AllRequirements

  val autoImport = Import

  import autoImport.PlovrKeys._
  import SbtWeb.autoImport._

  val basePlovrSettings : Seq[Setting[_]] = Seq(
      plovrTargets := Nil,
      plovrEntryPoints <<= (sourceDirectory in Compile)(base => ((base / "assets" ** "*.js") --- (base / "assets" ** "_*"))),
      plovrTmpDir := new File("/tmp"),

      plovr := {
System.out.println("compileJs called\n\n\n")
        val s = streams.value
        val jsFiles = plovrEntryPoints.value.get

        val newest = jsFiles.maxBy(_.lastModified)
        s.log.debug("Newest JS change: " + newest.lastModified + ": " + newest)
System.out.println("plovrTargets " + plovrTargets.value)
        plovrTargets.value flatMap {
          case (configFile, targetPath) => {
            val outputDir : File = (resourceManaged in Assets).value
            val targetFile : File = new File(outputDir, targetPath)
            val gzTarget : File = new File(targetFile.getAbsolutePath + ".gz")
System.out.println("last " + targetFile.lastModified)
System.out.println("newest " + newest.lastModified)
            s.log.debug("Existing JS timestamp:      " + targetFile.lastModified + ": " + targetFile)

            if (!targetFile.getParentFile.exists) {
              targetFile.getParentFile.mkdirs()
            }

            if (!targetFile.exists || targetFile.lastModified <= newest.lastModified) {
              IO.delete(targetFile)

              s.log.debug("Using plovr configuration file '" + configFile + "'")
System.out.println("Compiling " + jsFiles.size + " Javascript sources to " + targetFile.getAbsolutePath)
              s.log.info("Compiling " + jsFiles.size + " Javascript sources to " + targetFile.getAbsolutePath)

              PlayPlovrPlugin.plovrCompile(plovrTmpDir.value, targetFile, configFile, s) fold (
                // failure
                output => {
                  throw new RuntimeException(output.reverse.mkString("\n"))
                },
                // success
                _ => {
//                  IO.gzip(targetFile, gzTarget)
System.out.println("Javascript compiled, total size: " + targetFile.length / 1000 + " k")
//                  s.log.info("Javascript compiled, total size: " + targetFile.length / 1000 + " k, gz: " + gzTarget.length / 1000 + " k")
                }
              )

            }

            if (targetFile.length == 0) {
              IO.delete(targetFile)
              throw new RuntimeException("JavaScript compilation failed")
            }

//            Seq(targetFile, gzTarget)
            Seq(targetFile)
          }
        }
      }

    )

  override def projectSettings: Seq[Setting[_]] = 
      inConfig(Assets)(basePlovrSettings) ++
      Seq(
        plovr in Assets <<= (plovr in Assets).triggeredBy(compile in Compile)
      )

  private def plovrCompile(plovrTmpDir: File, targetFile: File, configFile: File, s: TaskStreams): Either[Seq[String], Seq[String]] = {
    targetFile.createNewFile()
    val plovrJar: File = ensurePlovrJar(plovrTmpDir, s)
    val command = "java -jar " + plovrJar.getAbsolutePath + " build " + configFile.getAbsolutePath
    s.log.debug("Plovr compile command: " + command)

    var success = true
    val compilerOutput = ArrayBuffer[String]()
    object outputLogger extends sbt.ProcessLogger {
      // stdout is piped to file
      def info(message: => String) { output(message) }

      // this is the actual output from the compiler
      def error(message: => String) { output(message) }

      def output(message: String) {
        if (message.startsWith("BUILD FAILED")) {
          success = false
        }

        if (!outputShouldBeFilteredOut(message)) {
          compilerOutput.append(message)
        }
      }

      def buffer[T](f: => T) = f
    }

    command #> targetFile ! outputLogger

    if (success) {
      Right(compilerOutput)
    } else {
      IO.delete(targetFile)
      Left(compilerOutput)
    }
  }

  private def outputShouldBeFilteredOut(message: String) =
    message.isEmpty ||
      message.contains("org.plovr.Manifest") ||
      message.contains(".DS_Store")

  private def ensurePlovrJar(plovrTmpDir: File, s: TaskStreams): File = {
    val plovrRelease = "plovr-81ed862.jar"
    val plovrJar = new File(plovrTmpDir, plovrRelease)
    if (!plovrJar.exists()) {
      val url: URL = new URL("http://plovr.googlecode.com/files/" + plovrRelease)
      val rbc: ReadableByteChannel = Channels.newChannel(url.openStream())
      val fos: FileOutputStream = new FileOutputStream(plovrJar)
      fos.getChannel().transferFrom(rbc, 0, java.lang.Long.MAX_VALUE)
      s.log.info("Downloaded " + url + " to " + plovrJar)
    }
    plovrJar
  }

}
