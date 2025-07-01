import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt.Keys.libraryDependencies
import sbt._

object AppDependencies {

  private val bootstrapVersion = "9.13.0"
  private val hmrcMongoVersion = "2.6.0"

  val compile = Seq(
    "uk.gov.hmrc"             %% "bootstrap-frontend-play-30" % bootstrapVersion,
    "uk.gov.hmrc"             %% "play-frontend-hmrc-play-30" % "12.7.0",
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-play-30"         % hmrcMongoVersion,
    "com.beachape"      %% "enumeratum-play"            % "1.8.2", //later version results in JsBoolean error for case classes when being used with BsonDocs
    /*
     * WARN! Choose this version carefully.
     * play-json-derived-codecs-10.1.0 was compiled for play 2.9,
     * whereas this project depends on play-3.0
     * This resulted in problems when running tests from Intellij Idea.
     * To workaround compatibility issues, play related transitive dependencies
     * were excluded from this library.
     * Once below PR is merged, there should be release a newer version
     * of this dependency compatible with play-3.0
     * https://github.com/julienrf/play-json-derived-codecs/pull/94
     */
    "org.julienrf"               %% "play-json-derived-codecs" % "10.1.0" excludeAll(ExclusionRule().withOrganization("com.typesafe.play")),
    "com.softwaremill.quicklens" %% "quicklens"                % "1.9.12",
    "org.typelevel"     %% "cats-core"                  % "2.13.0"

  )

  val test = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-30"     % bootstrapVersion            % Test,
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-test-play-30"    % hmrcMongoVersion            % Test,
    "org.jsoup"               %  "jsoup"                      % "1.21.1"            % Test,
  )

  val it = Seq.empty
}
