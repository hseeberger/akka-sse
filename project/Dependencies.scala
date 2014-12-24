import sbt._

object Version {
  val akka       = "2.3.8"
  val akkaHttp   = "1.0-M2"
  val scala      = "2.11.4"
  val scalaCheck = "1.11.3"
  val scalaTest  = "2.2.2"
}

object Library {
  val akkaHttp    = "com.typesafe.akka" %% "akka-http-experimental" % Version.akkaHttp
  val akkaTestkit = "com.typesafe.akka" %% "akka-testkit"           % Version.akka
  val scalaCheck  = "org.scalacheck"    %% "scalacheck"             % Version.scalaCheck
  val scalaTest   = "org.scalatest"     %% "scalatest"              % Version.scalaTest
}
