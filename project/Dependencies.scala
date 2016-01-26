import sbt._

object Version {
  val akka       = "2.4.2-RC1"
  val junit      = "4.12"
  val scala      = "2.11.7"
  val scalaCheck = "1.12.5"
  val scalaTest  = "2.2.6"
}

object Library {
  val akkaHttp    = "com.typesafe.akka" %% "akka-http-experimental" % Version.akka
  val akkaTestkit = "com.typesafe.akka" %% "akka-testkit"           % Version.akka
  val junit       = "junit"             %  "junit"                  % Version.junit
  val scalaCheck  = "org.scalacheck"    %% "scalacheck"             % Version.scalaCheck
  val scalaTest   = "org.scalatest"     %% "scalatest"              % Version.scalaTest
}
