// *****************************************************************************
// Projects
// *****************************************************************************

lazy val `akka-sse` =
  project
    .in(file("."))
    .aggregate(core, example, jmh)
    .enablePlugins(GitVersioning)
    .settings(settings)
    .settings(
      unmanagedSourceDirectories.in(Compile) := Seq.empty,
      unmanagedSourceDirectories.in(Test) := Seq.empty,
      publishArtifact := false
    )

lazy val core =
  project
    .enablePlugins(AutomateHeaderPlugin)
    .settings(settings)
    .settings(
      moduleName := "akka-sse",
      libraryDependencies ++= Seq(
        library.akkaHttp,
        library.akkaHttpTestkit % Test,
        library.junit           % Test,
        library.scalaCheck      % Test,
        library.scalaTest       % Test
      ),
      initialCommands := """|import de.heikoseeberger.akkasse._""".stripMargin
    )

lazy val example =
  project
    .dependsOn(core)
    .enablePlugins(AutomateHeaderPlugin)
    .settings(settings)
    .settings(
      libraryDependencies ++= Seq(
        library.akkaHttp
      ),
      publishArtifact := false
    )

lazy val jmh =
  project
    .dependsOn(core)
    .enablePlugins(AutomateHeaderPlugin, JmhPlugin)
    .settings(settings)
    .settings(
      libraryDependencies ++= Seq(
        library.akkaStream
      ),
      publishArtifact := false
    )

// *****************************************************************************
// Library dependencies
// *****************************************************************************

lazy val version =
  new {
    val akka       = "2.4.14"
    val akkaHttp   = "10.0.0"
    val junit      = "4.12"
    val scalaCheck = "1.13.4"
    val scalaTest  = "3.0.1"
  }

lazy val library =
  new {
    val akkaHttp        = "com.typesafe.akka" %% "akka-http"         % version.akkaHttp
    val akkaHttpTestkit = "com.typesafe.akka" %% "akka-http-testkit" % version.akkaHttp
    val akkaStream      = "com.typesafe.akka" %% "akka-stream"       % version.akka
    val junit           = "junit"             %  "junit"             % version.junit
    val scalaCheck      = "org.scalacheck"    %% "scalacheck"        % version.scalaCheck
    val scalaTest       = "org.scalatest"     %% "scalatest"         % version.scalaTest
}

// *****************************************************************************
// Settings
// *****************************************************************************

lazy val settings =
  commonSettings ++
  scalafmtSettings ++
  gitSettings ++
  headerSettings ++
  sonatypeSettings ++

lazy val commonSettings =
  Seq(
    scalaVersion := "2.12.1",
    crossScalaVersions := Seq(scalaVersion.value, "2.11.8"),
    organization := "de.heikoseeberger",
    licenses += ("Apache-2.0",
                 url("http://www.apache.org/licenses/LICENSE-2.0")),
    mappings.in(Compile, packageBin) +=
      baseDirectory.in(ThisBuild).value / "LICENSE" -> "LICENSE",
    scalacOptions ++= Seq(
      "-unchecked",
      "-deprecation",
      "-language:_",
      "-target:jvm-1.8",
      "-encoding", "UTF-8"
    ),
    javacOptions ++= Seq(
      "-source", "1.8",
      "-target", "1.8"
    ),
    unmanagedSourceDirectories.in(Compile) :=
      Seq(scalaSource.in(Compile).value, javaSource.in(Compile).value),
    unmanagedSourceDirectories.in(Test) :=
      Seq(scalaSource.in(Test).value, javaSource.in(Test).value)
)

lazy val scalafmtSettings =
  reformatOnCompileSettings ++
  Seq(
    formatSbtFiles := false,
    scalafmtConfig := Some(baseDirectory.in(ThisBuild).value / ".scalafmt.conf"),
    ivyScala := ivyScala.value.map(_.copy(overrideScalaVersion = sbtPlugin.value)) // TODO Remove once this workaround no longer needed (https://github.com/sbt/sbt/issues/2786)!
  )

lazy val gitSettings = Seq(
  git.useGitDescribe := true
)

import de.heikoseeberger.sbtheader.license.Apache2_0
lazy val headerSettings =
  Seq(
    headers := Map("scala" -> Apache2_0("2015", "Heiko Seeberger"))
  )

lazy val sonatypeSettings =
  Seq(
    homepage := Some(url("https://github.com/hseeberger/akka-sse")),
    scmInfo := Some(ScmInfo(url("https://github.com/hseeberger/akka-sse"),
                            "git@github.com:hseeberger/akka-sse.git")),
    developers += Developer("hseeberger",
                            "Heiko Seeberger",
                            "mail@heikoseeberger.de",
                            url("https://github.com/hseeberger")),
    pomIncludeRepository := (_ => false)
  )
