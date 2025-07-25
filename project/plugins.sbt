resolvers += MavenRepository("HMRC-open-artefacts-maven2", "https://open.artefacts.tax.service.gov.uk/maven2")
resolvers += Resolver.url("HMRC-open-artefacts-ivy2", url("https://open.artefacts.tax.service.gov.uk/ivy2"))(Resolver.ivyStylePatterns)
resolvers += Resolver.typesafeRepo("releases")

ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always

addSbtPlugin("uk.gov.hmrc"        % "sbt-auto-build"     % "3.24.0")
addSbtPlugin("uk.gov.hmrc"        % "sbt-distributables" % "2.6.0")
addSbtPlugin("org.playframework"  % "sbt-plugin"         % "3.0.8")
addSbtPlugin("org.scoverage"      % "sbt-scoverage"      % "2.0.12")
addSbtPlugin("com.github.sbt"     % "sbt-gzip"           % "2.0.0")
addSbtPlugin("uk.gov.hmrc"        % "sbt-sass-compiler"  % "0.12.0")
addSbtPlugin("org.scalastyle"     %% "scalastyle-sbt-plugin" % "1.0.0")
addSbtPlugin("org.wartremover"    %  "sbt-wartremover"       % "3.3.0")
addSbtPlugin("com.timushev.sbt"   %  "sbt-updates"           % "0.6.3")
addSbtPlugin("org.scalameta"      % "sbt-scalafmt"            % "2.5.4")

addDependencyTreePlugin
