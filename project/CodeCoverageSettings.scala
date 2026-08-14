import sbt.Setting
import scoverage.ScoverageKeys

object CodeCoverageSettings {

  private val excludedPackages: Seq[String] = Seq(
    "<empty>",
    "Reverse.*",
    "uk.gov.hmrc.BuildInfo",
    "app.*",
    "prod.*",
    ".*Routes.*",
    "testOnlyDoNotUseInAppConf.*",
    "uk.gov.hmrc.agentregistrationfrontend.testonly.*",
    "uk.gov.hmrc.agentregistration.shared.*"
  )

  val settings: Seq[Setting[_]] = Seq(
    //    ScoverageKeys.coverageEnabled := true, // breaks when run through jenkins see https://github.com/scoverage/sbt-scoverage/issues/84#issuecomment-263026890
    ScoverageKeys.coverageExcludedPackages := excludedPackages.mkString(","),
    ScoverageKeys.coverageMinimumStmtTotal := 90,
    ScoverageKeys.coverageFailOnMinimum := false,
    ScoverageKeys.coverageHighlighting := true
  )
}
