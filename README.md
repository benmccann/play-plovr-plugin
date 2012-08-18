Play Plovr plugin
=================

A [Plovr](http://plovr.com/) plugin for [Play Framework 2.x](https://github.com/playframework/Play20/).

Licensed under the [Apache 2 Software License](http://www.apache.org/licenses/LICENSE-2.0.html).

This plugin allows you to easily use Google's [Closure Compiler](https://developers.google.com/closure/compiler/) and [Closure Library](http://closure-library.googlecode.com/svn/docs/index.html) with Play 2. The Closure Compiler support which ships with Play 2 does not allow for easy use of the Closure Library.

To use, create a [plovr configuration file](http://plovr.com/docs.html).

Then, in your project/plugins.sbt, add the plug-in:

    addSbtPlugin("com.benmccann" % "play-plovr-plugin" % "0.1")

Finally, in your project/Build.scala, add the PlayPlovrPlugin.defaultPlovrSettings and set the path to plovrConfiguration and the plovrTargetFile:

    import com.benmccann.playplovr.PlayPlovrPlugin
    import com.benmccann.playplovr.PlayPlovrPlugin._
    
    val main = PlayProject(
      appName,
      appVersion,
      frontendDeps,
      file("frontend"),
      JAVA).settings(PlayPlovrPlugin.defaultPlovrSettings ++ Seq(
    
        // my Play custom settings
    
        .
        .
        .
    
        // project-specific plovr settings
        plovrConfiguration <<= baseDirectory(_ / "plovr" /  "plovr.json"),
        plovrTargetFile <<= (resourceManaged)(_ / "main" / "public" / "javascripts" / "compiled.js")
    
      ): _*
    ).dependsOn(dataProject).aggregate(dataProject)
