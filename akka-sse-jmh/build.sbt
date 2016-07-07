libraryDependencies ++= Vector(
  Library.akkaStream
)

initialCommands := """|import de.heikoseeberger.akkasse._""".stripMargin

publishArtifact := false
