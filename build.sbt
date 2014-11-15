lazy val akkaSse = project.in(file("."))

name := "akka-sse"

libraryDependencies ++= List(
)

initialCommands := """|import de.heikoseeberger.akkasse._""".stripMargin
