import sbt._

object Version {
  final val Akka              = "2.4.11"
  final val AkkaStreamContrib = "0.2"
  final val Junit             = "4.12"
  final val Scala             = "2.11.8"
  final val ScalaCheck        = "1.13.2"
  final val ScalaTest         = "3.0.0"
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
