import sbt._

object Version {
  val akka       = "2.3.10"
  val akkaHttp   = "1.0-RC1"
  val junit      = "4.12"
  val scala      = "2.11.6"
  val scalaCheck = "1.12.2"
  val scalaTest  = "2.2.4"
}

object Library {
  val akkaActor   = "com.typesafe.akka" %% "akka-actor"                   % Version.akka
  val akkaHttp    = "com.typesafe.akka" %% "akka-http-scala-experimental" % Version.akkaHttp
  val akkaTestkit = "com.typesafe.akka" %% "akka-testkit"                 % Version.akka
  val junit       = "junit"             %  "junit"                        % Version.junit
  val scalaCheck  = "org.scalacheck"    %% "scalacheck"                   % Version.scalaCheck
  val scalaTest   = "org.scalatest"     %% "scalatest"                    % Version.scalaTest
}
