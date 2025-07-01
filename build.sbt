import uk.gov.hmrc.DefaultBuildSettings

val strictBuilding: SettingKey[Boolean] = StrictBuilding.strictBuilding //defining here so it can be set before running sbt like `sbt 'set Global / strictBuilding := true' ...`
StrictBuilding.strictBuildingSetting

ThisBuild / majorVersion := 0
ThisBuild / scalaVersion := "2.13.16"

lazy val microservice = Project("agent-registration-frontend", file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin)
  .settings(
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    pipelineStages := Seq(gzip),
    Compile / doc / scalacOptions := Seq(), //this will allow to have warnings in `doc` task
    Test / doc / scalacOptions := Seq(), //this will allow to have warnings in `doc` task
    scalacOptions ++= ScalaCompilerFlags.scalaCompilerOptions,
    scalacOptions ++= {
      if (StrictBuilding.strictBuilding.value) ScalaCompilerFlags.strictScalaCompilerOptions else Nil
    },
    pipelineStages := Seq(gzip),
    Compile / scalacOptions -= "utf8",
    Test / parallelExecution := true,
    routesImport ++= Seq(
          "uk.gov.hmrc.agentregistrationfrontend"
      )
  )
  .settings(CodeCoverageSettings.settings: _*)
  .settings(resolvers += Resolver.jcenterRepo)
  .settings(CodeCoverageSettings.settings *)
  .settings(commands ++= SbtCommands.commands)
  .settings(SbtUpdatesSettings.sbtUpdatesSettings *)
  .settings(CodeCoverageSettings.settings *)
  .settings(WartRemoverSettings.wartRemoverSettings)
  .settings(PlayKeys.playDefaultPort := 22201)
