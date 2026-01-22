import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt.Keys.libraryDependencies
import sbt._

object AppDependencies {

  private val bootstrapVersion = "10.5.0"
  private val hmrcMongoVersion = "2.12.0"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc" %% "bootstrap-frontend-play-30" % bootstrapVersion,
    "uk.gov.hmrc" %% "play-frontend-hmrc-play-30" % "12.28.0",
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-30" % hmrcMongoVersion,
    "com.softwaremill.quicklens" %% "quicklens" % "1.9.12", // Updated for Scala 3 compatibility
    "uk.gov.hmrc" %% "play-conditional-form-mapping-play-30" % "3.4.0",
    "uk.gov.hmrc.objectstore" %% "object-store-client-play-30" % "2.5.0",
    "uk.gov.hmrc" %% "domain-test-play-30" % "13.0.0" // Needed in this scope for UTR generation in our test-only GRS stub
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc" %% "bootstrap-test-play-30" % bootstrapVersion % Test,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-test-play-30" % hmrcMongoVersion % Test,
    "org.jsoup" % "jsoup" % "1.22.1" % Test
  )

}
