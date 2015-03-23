lazy val root = project
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

name := "root"

unmanagedSourceDirectories in Compile := Nil
unmanagedSourceDirectories in Test := Nil

publishArtifact := false
