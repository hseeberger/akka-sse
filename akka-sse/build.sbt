libraryDependencies ++= Vector(
  Library.akkaHttp,
  Library.akkaHttpTestkit % "test",
  Library.junit           % "test",
  Library.scalaCheck      % "test",
  Library.scalaTest       % "test"
)

initialCommands := """|import de.heikoseeberger.akkasse._""".stripMargin
