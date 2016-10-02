lazy val `akka-sse-root` =
  project
    .in(file("."))
    .aggregate(`akka-sse`, `akka-sse-example`, `akka-sse-jmh`)
    .enablePlugins(GitVersioning)

lazy val `akka-sse` =
  project
    .enablePlugins(AutomateHeaderPlugin)

lazy val `akka-sse-example` =
  project
    .dependsOn(`akka-sse`)
    .enablePlugins(AutomateHeaderPlugin)

lazy val `akka-sse-jmh` =
  project
    .dependsOn(`akka-sse`)
    .enablePlugins(JmhPlugin)

unmanagedSourceDirectories.in(Compile) := Vector.empty
unmanagedSourceDirectories.in(Test)    := Vector.empty

publishArtifact := false
