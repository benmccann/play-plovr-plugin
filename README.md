Play Plovr plugin
=================

A [Plovr](http://plovr.com/) plugin for [Play Framework 2.x](https://github.com/playframework/Play20/).

Licensed under the [Apache 2 Software License](http://www.apache.org/licenses/LICENSE-2.0.html).

This plugin allows you to easily use Google's [Closure Compiler](https://developers.google.com/closure/compiler/) and [Closure Library](http://closure-library.googlecode.com/svn/docs/index.html) with Play 2. The Closure Compiler support which ships with Play 2 does not allow for easy use of the Closure Library.

To use, create a [plovr configuration file](http://plovr.com/docs.html).

Then, in your project/plugins.sbt, add the plug-in:

    addSbtPlugin("com.benmccann" % "play-plovr-plugin" % "0.4")

The latest version works with Play 2.2. The last version tested with Play 2.1 was 0.3.4.

Finally, in your project/Build.scala, add the PlayPlovrPlugin.defaultPlovrSettings and set the path to plovrConfiguration and the plovrTargetFile:

    import com.benmccann.playplovr.PlayPlovrPlugin
    import com.benmccann.playplovr.PlayPlovrPlugin._
    
    val main = play.Project(
      appName,
      appVersion,
      frontendDeps,
      file("frontend")).settings(PlayPlovrPlugin.defaultPlovrSettings ++ Seq(
    
        // my Play custom settings
    
        .
        .
        .
    
        // project-specific plovr settings
        plovrTargets <<= baseDirectory { base => Seq(
          base / "plovr" /  "plovr.json" -> "public/javascripts/compiled.js"
        )
    
      ): _*
    )

This will compile JavaScript found under app/assets. When you run Play in dev mode, you can dynamically compile and load your JavaScript via a server running locally on [port 9810](http://localhost:9810). Or, in production mode, you can load the optimized JavaScript from a compiled static file:

    @if(play.Play.application().isDev()) {
      <script type="text/javascript" src="http://localhost:9810/compile?id=myPlovrConfig&mode=SIMPLE&pretty-print=true"></script>
    } else {
      <script type="text/javascript" src="@routes.Assets.at("javascripts/compiled.js")"></script>
    }
