lazy val akkaSse = project.in(file("."))

name := "akka-sse"

libraryDependencies ++= List(
  Library.akkaHttp,
  Library.junit       % "test",
  Library.akkaTestkit % "test",
  Library.scalaCheck  % "test",
  Library.scalaTest   % "test"
)

initialCommands := """|import de.heikoseeberger.akkasse._""".stripMargin

// TODO Remove once Scala 2.11.6 has been fully released!
resolvers += "Scala 2.11.6 staging repo" at "https://oss.sonatype.org/content/repositories/orgscala-lang-1184"
