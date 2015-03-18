import bintray. { Keys => BintrayKeys }
import bintray.Plugin._
import com.typesafe.sbt.SbtGit._
import com.typesafe.sbt.SbtScalariform._
import de.heikoseeberger.sbtheader.HeaderPlugin
import de.heikoseeberger.sbtheader.license.Apache2_0
import sbt._
import sbt.Keys._
import scalariform.formatter.preferences._

object Build extends AutoPlugin {

  override def requires = plugins.JvmPlugin

  override def trigger = allRequirements

  override def projectSettings =
    scalariformSettings ++
    versionWithGit ++
    bintrayPublishSettings ++
    List(
      // Core settings
      organization := "de.heikoseeberger",
      scalaVersion := Version.scala,
      crossScalaVersions := Version.crossScala,
      scalacOptions ++= List(
        "-unchecked",
        "-deprecation",
        "-language:_",
        "-target:jvm-1.7",
        "-encoding", "UTF-8"
      ),
      javacOptions ++= List(
        "-source", "1.8",
        "-target", "1.8"
      ),
      licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0")),
      pomExtra := <url>https://github.com/hseeberger/akka-sse</url>
                  <scm>
                    <url>https://github.com/hseeberger/akka-sse</url>
                    <connection>scm:git:git://github.com/hseeberger/akka-sse.git</connection>
                  </scm>
                  <developers>
                    <developer>
                      <id>hseeberger</id>
                      <name>Heiko Seeberger</name>
                      <url>http://heikoseeberger.de</url>
                    </developer>
                  </developers>,
      // Scalariform settings
      ScalariformKeys.preferences := ScalariformKeys.preferences.value
        .setPreference(AlignArguments, true)
        .setPreference(AlignParameters, true)
        .setPreference(AlignSingleLineCaseStatements, true)
        .setPreference(AlignSingleLineCaseStatements.MaxArrowIndent, 100)
        .setPreference(DoubleIndentClassDeclaration, true),
      // Git settings
      git.baseVersion := "0.8.0",
      // Header settings
      HeaderPlugin.autoImport.headers := Map("scala" -> Apache2_0("2015", "Heiko Seeberger"))
    )
}
