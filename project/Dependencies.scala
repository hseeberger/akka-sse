import sbt._

object Version {
  final val Akka              = "2.4.12"
  final val AkkaHttp          = "10.0.0-RC2"
  final val AkkaStreamContrib = "0.6"
  final val Junit             = "4.12"
  final val Scala             = "2.12.0"
  final val ScalaCheck        = "1.13.4"
  final val ScalaTest         = "3.0.0"
}

object Library {
  val akkaHttp          = "com.typesafe.akka" %% "akka-http"           % Version.AkkaHttp
  val akkaHttpTestkit   = "com.typesafe.akka" %% "akka-http-testkit"   % Version.AkkaHttp
  val akkaStream        = "com.typesafe.akka" %% "akka-stream"         % Version.Akka
  val akkaStreamContrib = "com.typesafe.akka" %% "akka-stream-contrib" % Version.AkkaStreamContrib
  val junit             = "junit"             %  "junit"               % Version.Junit
  val scalaCheck        = "org.scalacheck"    %% "scalacheck"          % Version.ScalaCheck
  val scalaTest         = "org.scalatest"     %% "scalatest"           % Version.ScalaTest
}
