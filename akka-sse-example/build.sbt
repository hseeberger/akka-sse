name := "akka-sse-example"

libraryDependencies ++= List(
  Library.akkaActor,
  Library.akkaHttp
)

initialCommands := """|import de.heikoseeberger.akkasse.example._""".stripMargin
