package uk.gov.hmrc.agentregistration.shared

import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.Format
import play.api.libs.json.Json
import play.api.libs.json.Reads
import play.api.libs.json.__
import play.api.libs.json.__

final case class CompaniesHouseNameQuery(
  firstName: String,
  lastName: String
)

object CompaniesHouseNameQuery:
    implicit val format: Format[CompaniesHouseNameQuery] = Json.format[CompaniesHouseNameQuery]
    def unapply(q: CompaniesHouseNameQuery): Option[(String, String)] = Some((q.firstName, q.lastName))

final case class CompaniesHouseDateOfBirth(
                                      day: Option[Int],
                                      month: Int,
                                      year: Int
                                    )

final case class CompaniesHouseOfficer(
                                  name: String,
                                  dateOfBirth: Option[CompaniesHouseDateOfBirth]
                                )

object CompaniesHouseDateOfBirth {
  implicit val format: Format[CompaniesHouseDateOfBirth] = Json.format[CompaniesHouseDateOfBirth]
}

object CompaniesHouseOfficer {

  implicit val reads: Reads[CompaniesHouseOfficer] =
    ((__ \ "name").read[String] and
      (__ \ "date_of_birth").readNullable[CompaniesHouseDateOfBirth])(CompaniesHouseOfficer.apply _)
}