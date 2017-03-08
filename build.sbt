// *****************************************************************************
// Projects
// *****************************************************************************

lazy val `akka-sse` =
  project
    .in(file("."))
    .enablePlugins(GitVersioning)
    .aggregate(core, example, jmh)
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
      name := "akka-sse",
      libraryDependencies ++= Seq(
        library.akkaHttp,
        library.akkaHttpTestkit % Test,
        library.junit           % Test,
        library.scalaCheck      % Test,
        library.scalaTest       % Test
      )
    )

lazy val example =
  project
    .enablePlugins(AutomateHeaderPlugin)
    .dependsOn(core)
    .settings(settings)
    .settings(
      libraryDependencies ++= Seq(
        library.akkaHttp
      ),
      publishArtifact := false
    )

lazy val jmh =
  project
    .enablePlugins(AutomateHeaderPlugin, JmhPlugin)
    .dependsOn(core)
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


lazy val library =
  new {
    object Version {
      val akka       = "2.4.17"
      val akkaHttp   = "10.0.4"
      val junit      = "4.12"
      val scalaCheck = "1.13.4"
      val scalaTest  = "3.0.1"
    }
    val akkaHttp        = "com.typesafe.akka" %% "akka-http"         % Version.akkaHttp
    val akkaHttpTestkit = "com.typesafe.akka" %% "akka-http-testkit" % Version.akkaHttp
    val akkaStream      = "com.typesafe.akka" %% "akka-stream"       % Version.akka
    val junit           = "junit"             %  "junit"             % Version.junit
    val scalaCheck      = "org.scalacheck"    %% "scalacheck"        % Version.scalaCheck
    val scalaTest       = "org.scalatest"     %% "scalatest"         % Version.scalaTest
}

// *****************************************************************************
// Settings
// *****************************************************************************

lazy val settings =
  commonSettings ++
  gitSettings ++
  headerSettings ++
  sonatypeSettings

lazy val commonSettings =
  Seq(
    // scalaVersion from .travis.yml
    // crossScalaVersions from .travis.yml
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

lazy val gitSettings =
  Seq(
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
