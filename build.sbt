lazy val akkaSseRoot = project
  .in(file("."))
  .aggregate(akkaSse, akkaSseExample)
  .enablePlugins(GitVersioning)

lazy val akkaSse = project
  .in(file("akka-sse"))
  .enablePlugins(AutomateHeaderPlugin)

lazy val akkaSseExample = project
  .in(file("akka-sse-example"))
  .dependsOn(akkaSse)
  .enablePlugins(AutomateHeaderPlugin)

lazy val akkaSseJmh = project
  .in(file("akka-sse-jmh"))
  .dependsOn(akkaSse)
  .enablePlugins(JmhPlugin)

name := "akka-sse-root"

unmanagedSourceDirectories.in(Compile) := Nil
unmanagedSourceDirectories.in(Test)    := Nil

publishArtifact := false
