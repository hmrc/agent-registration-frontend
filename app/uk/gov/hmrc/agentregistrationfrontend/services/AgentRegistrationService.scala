package uk.gov.hmrc.agentregistrationfrontend.services

import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentregistration.shared.CompaniesHouseNameQuery
import uk.gov.hmrc.agentregistration.shared.CompaniesHouseOfficer
import uk.gov.hmrc.agentregistrationfrontend.connectors.AgentRegistrationConnector
import uk.gov.hmrc.agentregistrationfrontend.util.RequestAwareLogging

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class AgentRegistrationService @Inject() (
  agentRegistrationConnector: AgentRegistrationConnector,
  applicationFactory: ApplicationFactory
)(using ec: ExecutionContext)
extends RequestAwareLogging:

  def searchCompaniesHouseOfficers(
                                    companiesHouseNameQuery: CompaniesHouseNameQuery
                                  )(using request: RequestHeader): Future[Seq[CompaniesHouseOfficer]] =
    agentRegistrationConnector.searchCompaniesHouseOfficers(companiesHouseNameQuery)
