lazy val root = project
  .copy(id = "root")
  .in(file("."))
  .aggregate(akkaSse, akkaSseExample, akkaSseJmh)
  .enablePlugins(GitVersioning)

lazy val akkaSse = project
  .copy(id = "akka-sse")
  .in(file("akka-sse"))
  .enablePlugins(AutomateHeaderPlugin)

lazy val akkaSseExample = project
  .copy(id = "akka-sse-example")
  .in(file("akka-sse-example"))
  .dependsOn(akkaSse)
  .enablePlugins(AutomateHeaderPlugin)

lazy val akkaSseJmh = project
  .copy(id = "akka-sse-jmh")
  .in(file("akka-sse-jmh"))
  .dependsOn(akkaSse)
  .enablePlugins(JmhPlugin)

name := "akka-sse-root"

unmanagedSourceDirectories.in(Compile) := Nil
unmanagedSourceDirectories.in(Test)    := Nil

publishArtifact := false
