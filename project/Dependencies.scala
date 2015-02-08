import sbt._

object Version {
  val akka       = "2.3.9"
  val akkaHttp   = "1.0-M3"
  val junit      = "4.12"
  val scala      = "2.11.5"
  val scalaCheck = "1.12.2"
  val scalaTest  = "2.2.3"
}

object Library {
  val akkaHttp    = "com.typesafe.akka" %% "akka-http-experimental" % Version.akkaHttp
  val akkaTestkit = "com.typesafe.akka" %% "akka-testkit"           % Version.akka
  val junit       = "junit"             %  "junit"                  % Version.junit
  val scalaCheck  = "org.scalacheck"    %% "scalacheck"             % Version.scalaCheck
  val scalaTest   = "org.scalatest"     %% "scalatest"              % Version.scalaTest
}
