lazy val `akka-sse-root` = project
  .in(file("."))
  .aggregate(`akka-sse`, `akka-sse-example`, `akka-sse-jmh`)
  .enablePlugins(GitVersioning)

lazy val `akka-sse` = project
  .in(file("akka-sse"))
  .enablePlugins(AutomateHeaderPlugin)

lazy val `akka-sse-example` = project
  .in(file("akka-sse-example"))
  .dependsOn(`akka-sse`)
  .enablePlugins(AutomateHeaderPlugin)

lazy val `akka-sse-jmh` = project
  .in(file("akka-sse-jmh"))
  .dependsOn(`akka-sse`)
  .enablePlugins(JmhPlugin)

unmanagedSourceDirectories.in(Compile) := Vector.empty
unmanagedSourceDirectories.in(Test)    := Vector.empty

publishArtifact := false
