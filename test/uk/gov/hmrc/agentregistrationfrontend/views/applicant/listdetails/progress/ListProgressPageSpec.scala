/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.agentregistrationfrontend.views.applicant.listdetails.progress

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ViewSpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata.TdTestOnly
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.listdetails.progress.ListProgressPage

class ListProgressPageSpec
extends ViewSpec:

  val viewTemplate: ListProgressPage = app.injector.instanceOf[ListProgressPage]
  val doc: Document = Jsoup.parse(
    viewTemplate(
      agentApplication = tdAll.agentApplicationGeneralPartnership.afterHowManyKeyIndividuals,
      existingList = List(
        tdAll.providedDetails.precreated,
        TdTestOnly.additionalIndividuals.secondIndividual.providedDetails.precreated,
        TdTestOnly.additionalIndividuals.thirdIndividual.providedDetails.afterFinished
      ),
      entityName = "Test Company Name"
    ).body
  )
  val docWhenComplete: Document = Jsoup.parse(
    viewTemplate(
      agentApplication = tdAll.agentApplicationGeneralPartnership.afterHowManyKeyIndividuals,
      existingList = List(
        tdAll.providedDetails.afterFinished,
        TdTestOnly.additionalIndividuals.secondIndividual.providedDetails.afterFinished,
        TdTestOnly.additionalIndividuals.thirdIndividual.providedDetails.afterFinished
      ),
      entityName = "Test Company Name"
    ).body
  )

  "ListProgressPage when individuals have yet to provide details" should:

    "have expected content" in:
      doc.mainContent shouldContainContent
        s"""
           |Check who has provided their details
           |HMRC needs information from everyone on this list
           |Awaiting details for 2 of 3 people
           |Name
           |Details provided
           |Test Name
           |No
           |Second Test Name
           |No
           |Third Test Name
           |Yes
           |What happens if someone cannot sign in?
           |Ideally, everyone on the list will sign in and provide personal details.
           |However, if there’s an exceptional reason why someone cannot sign in, we allow you to submit details on their behalf.
           |This might affect how we assess the risk of providing Test Company Name with an agent services account.
           |If you accept this risk, you can provide us with information on someone else’s behalf.
           |Return to task list
           |Save and come back later
           |"""
          .stripMargin

    "have the correct title" in:
      doc.title() shouldBe "HMRC needs information from everyone on this list - Apply for an agent services account - GOV.UK"

    "have the correct h1" in:
      doc.h1 shouldBe "HMRC needs information from everyone on this list"

  "ListProgressPage when all individuals have provided details" should:

    "have expected content without content and link for applicant to provide details" in:
      docWhenComplete.mainContent shouldContainContent
        s"""
           |Check who has provided their details
           |HMRC needs information from everyone on this list
           |Awaiting details for 0 of 3 people
           |Name
           |Details provided
           |Test Name
           |Yes
           |Second Test Name
           |Yes
           |Third Test Name
           |Yes
           |Return to task list
           |Save and come back later
           |"""
          .stripMargin

    "have the correct title" in:
      docWhenComplete.title() shouldBe "HMRC needs information from everyone on this list - Apply for an agent services account - GOV.UK"

    "have the correct h1" in:
      docWhenComplete.h1 shouldBe "HMRC needs information from everyone on this list"
