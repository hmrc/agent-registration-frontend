/*
 * Copyright 2026 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.listdetails.nonincorporated

import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.i18n.MessagesApi
import play.api.mvc.Call
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.agentregistration.shared.*
import uk.gov.hmrc.agentregistration.shared.AgentApplication.IsUnincorporatedPartnership
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.lists.IndividualName
import uk.gov.hmrc.agentregistration.shared.lists.NumberOfRequiredKeyIndividuals
import uk.gov.hmrc.agentregistration.shared.lists.SixOrMore
import uk.gov.hmrc.agentregistration.shared.util.Errors.getOrThrowExpectedDataMissing
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistrationfrontend.action.Actions.RequestWithData
import uk.gov.hmrc.agentregistrationfrontend.action.applicant.ApplicantActions.DataWithUnincorporatedPartnershipKeyIndividuals
import uk.gov.hmrc.agentregistrationfrontend.services.BusinessPartnerRecordService
import uk.gov.hmrc.agentregistrationfrontend.util.MessageKeys
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.listdetails.nonincorporated.EnterIndividualNameComplexPage
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.listdetails.nonincorporated.EnterIndividualNamePage

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class EnterIndividualNamePageRenderer @Inject() (
  enterIndividualNameSimplePage: EnterIndividualNamePage,
  enterIndividualNameComplexPage: EnterIndividualNameComplexPage,
  businessPartnerRecordService: BusinessPartnerRecordService,
  val messagesApi: MessagesApi
)(using ExecutionContext)
extends I18nSupport:

  /** The complex page introduces the rules to an applicant who has named nobody yet, so it is only shown for the first name of a six or more list. Every other
    * name gets the simple page.
    */
  def render(
    form: Form[IndividualName],
    formAction: Call
  )(using request: RequestWithData[DataWithUnincorporatedPartnershipKeyIndividuals]): Future[HtmlFormat.Appendable] =
    val existingList: List[IndividualProvidedDetails] = request.get
    request.get[NumberOfRequiredKeyIndividuals] match
      case sixOrMore: SixOrMore if existingList.isEmpty && sixOrMore.numberOfKeyIndividualsResponsibleForTaxMatters > 0 =>
        val agentApplication: IsUnincorporatedPartnership = request.get
        businessPartnerRecordService
          .getBusinessPartnerRecord(agentApplication.getUtr)
          .map: maybeBusinessPartnerRecord =>
            enterIndividualNameComplexPage(
              form = form,
              ordinalKey = MessageKeys.ordinalKey(
                existingSize = existingList.size,
                isOnlyOne = false // list size here can never be 1
              ),
              numberOfRequiredKeyIndividuals = sixOrMore,
              entityName = maybeBusinessPartnerRecord
                .map(_.getEntityName)
                .getOrThrowExpectedDataMissing(
                  "Business Partner Record is missing"
                ),
              formAction = formAction
            )
      case numberOfRequiredKeyIndividuals: NumberOfRequiredKeyIndividuals =>
        Future.successful(enterIndividualNameSimplePage(
          form = form,
          ordinalKey = MessageKeys.ordinalKey(
            existingSize = existingList.size,
            isOnlyOne = numberOfRequiredKeyIndividuals.numberOfIndividuals === 1
          ),
          formAction = formAction
        ))
