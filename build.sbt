lazy val akkaSse = project.in(file("."))

name := "akka-sse"

libraryDependencies ++= List(
  Library.akkaHttp,
  Library.scalaTest % "test"
)

initialCommands := """|import de.heikoseeberger.akkasse._""".stripMargin
