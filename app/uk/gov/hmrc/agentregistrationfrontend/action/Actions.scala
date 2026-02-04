/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.agentregistrationfrontend.action

import play.api.mvc.*
import play.api.mvc.Results.Redirect
import uk.gov.hmrc.agentregistration.shared.*
import uk.gov.hmrc.agentregistrationfrontend.controllers.AppRoutes
import uk.gov.hmrc.agentregistrationfrontend.forms.helpers.SubmissionHelper
import uk.gov.hmrc.auth.core.retrieve.Credentials

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

object Actions:

  export uk.gov.hmrc.agentregistrationfrontend.util.UniqueTuple.PresentIn
  export uk.gov.hmrc.agentregistrationfrontend.util.UniqueTuple.AbsentIn

  type RequestWithData[Data <: Tuple] = RequestWithDataCt[AnyContent, Data]

  type DataEmpty = EmptyTuple
  type DefaultRequest = RequestWithData[DataEmpty]
  type DefaultRequestCt[ContentType] = RequestWithDataCt[ContentType, DataEmpty]

  extension [
    ContentType,
    Data <: Tuple
  ](r: RequestWithDataCt[ContentType, Data])

    inline def agentApplication(using AgentApplication PresentIn Data): AgentApplication = r.get
    inline def agentApplicationGeneralPartnership(using AgentApplicationGeneralPartnership PresentIn Data): AgentApplicationGeneralPartnership = r.get
    inline def agentApplicationLimitedCompany(using AgentApplicationLimitedCompany PresentIn Data): AgentApplicationLimitedCompany = r.get
    inline def agentApplicationLimitedPartnership(using AgentApplicationLimitedPartnership PresentIn Data): AgentApplicationLimitedPartnership = r.get
    inline def agentApplicationLlp(using AgentApplicationLlp PresentIn Data): AgentApplicationLlp = r.get
    inline def agentApplicationScottishLimitedPartnership(using
      AgentApplicationScottishLimitedPartnership PresentIn Data
    ): AgentApplicationScottishLimitedPartnership = r.get
    inline def agentApplicationScottishPartnership(using AgentApplicationScottishPartnership PresentIn Data): AgentApplicationScottishPartnership = r.get
    inline def agentApplicationSoleTrader(using AgentApplicationSoleTrader PresentIn Data): AgentApplicationSoleTrader = r.get
    inline def credentials(using Credentials PresentIn Data): Credentials = r.get
    inline def internalUserId(using InternalUserId PresentIn Data): InternalUserId = r.get
    inline def groupId(using GroupId PresentIn Data): GroupId = r.get
    inline def businessPartnerRecordResponse(using BusinessPartnerRecordResponse PresentIn Data): BusinessPartnerRecordResponse = r.get
    inline def maybeBusinessPartnerRecordResponse(using Option[BusinessPartnerRecordResponse] PresentIn Data): Option[BusinessPartnerRecordResponse] = r.get
    inline def agentType(using AgentType PresentIn Data): AgentType = r.get

  extension [
    ContentType // B Represents Play Framework's Content Type parameter, commonly denoted as B
  ](a: Action[ContentType])(using ec: ExecutionContext)

    def mapResult(f: Request[ContentType] => Result => Result): Action[ContentType] =
      new Action[ContentType] {
        override def apply(request: Request[ContentType]): Future[Result] = a(request).map(f(request))

        override def parser: BodyParser[ContentType] = a.parser

        override def executionContext: ExecutionContext = a.executionContext
      }

  extension (a: Action[AnyContent])(using ec: ExecutionContext)
    /** Modifies the action result to handle "Save and Come Back Later" functionality. If the form submission contains a "Save and Come Back Later" action,
      * redirects to the Save and Come Back Later page. Otherwise, returns the original result unchanged.
      */
    def redirectIfSaveForLater: Action[AnyContent] = a.mapResult(request =>
      originalResult =>
        if SubmissionHelper.getSubmitAction(request).isSaveAndComeBackLater then Redirect(AppRoutes.apply.SaveForLaterController.show)
        else originalResult
    )
