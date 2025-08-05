import uk.gov.hmrc.DefaultBuildSettings

val strictBuilding: SettingKey[Boolean] = StrictBuilding.strictBuilding //defining here so it can be set before running sbt like `sbt 'set Global / strictBuilding := true' ...`
StrictBuilding.strictBuildingSetting
val playPort: Int = 22201
PlayKeys.playRunHooks += PlayRunHook(playPort)

ThisBuild / majorVersion := 0
ThisBuild / scalaVersion := "3.6.1"
ThisBuild / scalafmtOnCompile := true

lazy val microservice = Project("agent-registration-frontend", file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin)
  .settings(
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    pipelineStages := Seq(gzip),
    Compile / doc / scalacOptions := Seq(), //this will allow to have warnings in `doc` task
    Test / doc / scalacOptions := Seq(), //this will allow to have warnings in `doc` task
    scalacOptions -= "-Wunused:all",
    scalacOptions ++= ScalaCompilerFlags.scalaCompilerOptions,
    scalacOptions ++= {
      if (StrictBuilding.strictBuilding.value) ScalaCompilerFlags.strictScalaCompilerOptions else Nil
    },
    pipelineStages := Seq(gzip),
    Test / parallelExecution := true,
    routesImport ++= Seq(
          "uk.gov.hmrc.agentregistrationfrontend"
      )
  )
  .settings(CodeCoverageSettings.settings: _*)
  .settings(CodeCoverageSettings.settings *)
  .settings(commands ++= SbtCommands.commands)
  .settings(SbtUpdatesSettings.sbtUpdatesSettings *)
  .settings(CodeCoverageSettings.settings *)
//  .settings(WartRemoverSettings.wartRemoverSettings)
  .settings(PlayKeys.playDefaultPort := playPort)
  .settings(
    TwirlKeys.templateImports ++= Seq(
      "uk.gov.hmrc.govukfrontend.views.html.components._",
      "uk.gov.hmrc.hmrcfrontend.views.html.components._"
    )
  )
