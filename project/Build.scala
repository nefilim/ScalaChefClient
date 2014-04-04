import sbt._
import Keys._
import net.virtualvoid.sbt.graph.Plugin._

object MyBuild extends Build {

  // prompt shows current project
  override lazy val settings = super.settings :+ {
    shellPrompt := { s => Project.extract(s).currentProject.id + " > " }
  }

  // TODO move these vals to V
  val myScalaVersion = "2.10.4"

  lazy val buildSettings = Defaults.defaultSettings ++ graphSettings ++ Seq( // must include Defaults.defaultSettings somewhere (early) in the chain
    organization := "org.nefilim",
    scalaVersion := myScalaVersion
  )

  lazy val defaultSettings = buildSettings ++ publishSettings ++ Seq(
    scalacOptions in Compile ++= Seq("-encoding", "UTF-8", "-target:jvm-1.7", "-deprecation", "-feature", "-unchecked", "-Xlog-reflective-calls"),
    testOptions in Test += Tests.Argument("-oDF")
  )

  lazy val publishSettings = Seq()

  lazy val chefClientProject = Project(
    id = "chefclient",
    base = file("."),
    settings = defaultSettings ++ Seq(
      publish := { },
      libraryDependencies ++= Dependencies.chefClient
    )
  )

}
