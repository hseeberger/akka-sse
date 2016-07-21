import com.typesafe.sbt.GitPlugin
import de.heikoseeberger.sbtheader.HeaderPlugin
import de.heikoseeberger.sbtheader.license.Apache2_0
import org.scalafmt.sbt.ScalaFmtPlugin
import sbt._
import sbt.Keys._
import sbt.plugins.JvmPlugin

object Build extends AutoPlugin {

  override def requires = JvmPlugin && HeaderPlugin && GitPlugin && ScalaFmtPlugin

  override def trigger = allRequirements

  override def projectSettings =
    ScalaFmtPlugin.autoImport.reformatOnCompileSettings ++
    Vector(
      // Core settings
      organization := "de.heikoseeberger",
      licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0")),
      homepage := Some(url("https://github.com/hseeberger/akka-sse")),
      pomIncludeRepository := (_ => false),
      pomExtra := <scm>
                    <url>https://github.com/hseeberger/akka-sse</url>
                    <connection>scm:git:git@github.com:hseeberger/akka-sse.git</connection>
                  </scm>
                  <developers>
                    <developer>
                      <id>hseeberger</id>
                      <name>Heiko Seeberger</name>
                      <url>http://heikoseeberger.de</url>
                    </developer>
                  </developers>,
      scalaVersion := Version.Scala,
      crossScalaVersions := Vector(Version.Scala),
      scalacOptions ++= Vector(
        "-unchecked",
        "-deprecation",
        "-language:_",
        "-target:jvm-1.8",
        "-encoding", "UTF-8"
      ),
      javacOptions ++= Vector(
        "-source", "1.8",
        "-target", "1.8"
      ),
      unmanagedSourceDirectories.in(Compile) := Vector(scalaSource.in(Compile).value),
      unmanagedSourceDirectories.in(Test) := Vector(scalaSource.in(Test).value),

      // scalafmt settings
      ScalaFmtPlugin.autoImport.scalafmtConfig := Some(baseDirectory.in(ThisBuild).value / ".scalafmt"),

      // Git settings
      GitPlugin.autoImport.git.useGitDescribe := true,

      // Header settings
      HeaderPlugin.autoImport.headers := Map("scala" -> Apache2_0("2015", "Heiko Seeberger"))
    )
}
