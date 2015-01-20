lazy val akkaSse = project.in(file("."))

name := "akka-sse"

libraryDependencies ++= List(
  Library.akkaHttp,
  Library.junit      % "test",
  Library.scalaCheck % "test",
  Library.scalaTest  % "test"
)

initialCommands := """|import de.heikoseeberger.akkasse._""".stripMargin
