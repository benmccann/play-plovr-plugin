play-plovr-plugin
=================

A Plovr plugin for Play Framework 2.x.

Licensed under the [Apache 2 Software License](http://www.apache.org/licenses/LICENSE-2.0.html).

To use, add the PlayPlovrPlugin.defaultPlovrSettings and set the path to plovrConfiguration and the plovrTargetFile.

Example:

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
