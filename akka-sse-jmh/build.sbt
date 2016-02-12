name := "akka-sse-jmh"

libraryDependencies ++= List(
  Library.akkaStream
)

initialCommands := """|import de.heikoseeberger.akkasse._""".stripMargin

publishArtifact := false
