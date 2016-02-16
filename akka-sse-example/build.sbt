name := "akka-sse-example"

libraryDependencies ++= Vector(
  Library.akkaHttp
)

initialCommands := """|import de.heikoseeberger.akkasse.example._""".stripMargin

publishArtifact := false
