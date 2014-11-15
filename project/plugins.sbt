addSbtPlugin("com.danieltrinh"   % "sbt-scalariform" % "1.3.0")
addSbtPlugin("com.github.gseitz" % "sbt-release"     % "0.8.5")
addSbtPlugin("me.lessis"         % "bintray-sbt"     % "0.1.2")

resolvers += Resolver.url("bintraysbt-plugin-releases", url("http://dl.bintray.com/content/sbt/sbt-plugin-releases"))(Resolver.ivyStylePatterns)
