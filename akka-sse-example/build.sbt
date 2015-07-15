name := "akka-sse-example"

libraryDependencies ++= List(
  Library.akkaHttp
)

initialCommands := """|import de.heikoseeberger.akkasse.example._""".stripMargin

publishArtifact := false
