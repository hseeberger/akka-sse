import sbt._

object Version {
  final val Akka              = "2.4.6"
  final val AkkaStreamContrib = "0.1"
  final val Junit             = "4.12"
  final val Scala             = "2.11.8"
  final val ScalaCheck        = "1.12.5"
  final val ScalaTest         = "2.2.6"
}

object Library {
  val akkaHttp          = "com.typesafe.akka" %% "akka-http-experimental" % Version.Akka
  val akkaHttpTestkit   = "com.typesafe.akka" %% "akka-http-testkit"      % Version.Akka
  val akkaStream        = "com.typesafe.akka" %% "akka-stream"            % Version.Akka
  val akkaStreamContrib = "com.typesafe.akka" %% "akka-stream-contrib"    % Version.AkkaStreamContrib
  val junit             = "junit"             %  "junit"                  % Version.Junit
  val scalaCheck        = "org.scalacheck"    %% "scalacheck"             % Version.ScalaCheck
  val scalaTest         = "org.scalatest"     %% "scalatest"              % Version.ScalaTest
}
