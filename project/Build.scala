import sbt._
import sbt.Keys._
import net.virtualvoid.sbt.graph.Plugin._
import scala.Some
import sbtrelease.ReleasePlugin._

object MyBuild extends Build {

  // prompt shows current project
  override lazy val settings = super.settings :+ {
    shellPrompt := { s => Project.extract(s).currentProject.id + " > " }
  }

  // TODO move these vals to V
  val myScalaVersion = "2.10.4"

  lazy val buildSettings = Defaults.defaultSettings ++ graphSettings ++ Seq( // must include Defaults.defaultSettings somewhere (early) in the chain
    organization := "org.nefilim",
    scalaVersion := myScalaVersion,
    version := "0.1"
  )

  lazy val publishSettings = Seq(
    publishArtifact in Test := false,
    publishTo := {
      val nexus = "https://oss.sonatype.org/"
      if (isSnapshot.value)
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases"  at nexus + "service/local/staging/deploy/maven2")
    },
    pomExtra := (
      <url>https://github.com/nefilim/ScalaChefClient</url>
        <licenses>
          <license>
            <name>GNU General Public License (GPL)</name>
            <url>http://www.gnu.org/licenses/gpl.txt</url>
            <distribution>repo</distribution>
          </license>
        </licenses>
        <scm>
          <url>https://github.com/nefilim/ScalaChefClient.git</url>
          <connection>scm:git:https://github.com/nefilim/ScalaChefClient.git</connection>
        </scm>
        <developers>
          <developer>
            <id>nefilim</id>
            <name>Peter van Rensburg</name>
            <url>http://www.nefilim.org</url>
          </developer>
        </developers>)
  )

  lazy val defaultSettings = buildSettings ++ publishSettings ++ releaseSettings ++ Seq(
    scalacOptions in Compile ++= Seq("-encoding", "UTF-8", "-target:jvm-1.7", "-deprecation", "-feature", "-unchecked", "-Xlog-reflective-calls"),
    testOptions in Test += Tests.Argument("-oDF")
  )

  lazy val chefClientProject = Project(
    id = "chefclient",
    base = file("."),
    settings = defaultSettings ++ Seq(
      publish := { },
      libraryDependencies ++= Dependencies.chefClient
    )
  )

}
