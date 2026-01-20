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

package uk.gov.hmrc.agentregistrationfrontend.testonly.controllers

import play.api.data.FieldMapping
import play.api.data.Form
import play.api.data.Forms
import play.api.data.Forms.mapping
import play.api.data.Forms.nonEmptyText
import play.api.data.Forms.optional
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import play.api.mvc.Request
import uk.gov.hmrc.agentregistration.shared.BusinessType
import uk.gov.hmrc.agentregistration.shared.Crn
import uk.gov.hmrc.agentregistration.shared.CtUtr
import uk.gov.hmrc.agentregistration.shared.Nino
import uk.gov.hmrc.agentregistration.shared.SaUtr
import uk.gov.hmrc.agentregistration.shared.SafeId
import uk.gov.hmrc.agentregistration.shared.BusinessType.*
import uk.gov.hmrc.agentregistration.shared.BusinessType.Partnership.*
import uk.gov.hmrc.agentregistration.shared.businessdetails.CompanyProfile
import uk.gov.hmrc.agentregistration.shared.businessdetails.FullName
import uk.gov.hmrc.agentregistration.shared.companieshouse.ChroAddress
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistrationfrontend.action.Actions
import uk.gov.hmrc.agentregistrationfrontend.action.AuthorisedRequest
import uk.gov.hmrc.agentregistrationfrontend.action.FormValue
import uk.gov.hmrc.agentregistrationfrontend.controllers.FrontendController

import javax.inject.Inject
import javax.inject.Singleton
import uk.gov.hmrc.agentregistrationfrontend.forms.formatters.FormatterFactory
import uk.gov.hmrc.agentregistrationfrontend.model.grs.JourneyId
import uk.gov.hmrc.agentregistrationfrontend.model.grs.JourneyConfig
import uk.gov.hmrc.agentregistrationfrontend.model.grs.JourneyData
import uk.gov.hmrc.agentregistrationfrontend.model.grs.Registration
import uk.gov.hmrc.agentregistrationfrontend.model.grs.RegistrationStatus
import uk.gov.hmrc.agentregistrationfrontend.model.grs.RegistrationStatus.GrsNotCalled
import uk.gov.hmrc.agentregistrationfrontend.model.grs.RegistrationStatus.GrsRegistered
import uk.gov.hmrc.agentregistrationfrontend.testonly.connectors.AgentsExternalStubsConnector
import uk.gov.hmrc.agentregistrationfrontend.testonly.model.AddressDetails
import uk.gov.hmrc.agentregistrationfrontend.testonly.model.BusinessPartnerRecord
import uk.gov.hmrc.agentregistrationfrontend.testonly.model.ContactDetails
import uk.gov.hmrc.agentregistrationfrontend.testonly.model.Individual
import uk.gov.hmrc.agentregistrationfrontend.testonly.model.Organisation
import uk.gov.hmrc.agentregistrationfrontend.testonly.views.html.GrsStub
import uk.gov.hmrc.domain.SaUtrGenerator
import uk.gov.voa.play.form.ConditionalMappings.isEqual
import uk.gov.voa.play.form.ConditionalMappings.mandatoryIf
import uk.gov.voa.play.form.conditionOpts
import uk.gov.hmrc.domain.NinoGenerator
import scala.concurrent.Future

import java.time.LocalDate
import java.util.UUID

@Singleton
class GrsStubController @Inject() (
  mcc: MessagesControllerComponents,
  actions: Actions,
  view: GrsStub,
  agentsExternalStubsConnector: AgentsExternalStubsConnector
)
extends FrontendController(mcc, actions):

  def showGrsData(
    businessType: BusinessType,
    journeyId: JourneyId
  ): Action[AnyContent] = actions
    .Applicant
    .authorised:
      implicit request =>
        val prefilledForm =
          request.session.get(journeyId.value).map(data => Json.parse(data).as[JourneyData]) match {
            case Some(data) => form(businessType).fill(data)
            case _ => formWithDefaults(businessType)
          }
        Ok(view(
          prefilledForm,
          businessType,
          journeyId
        ))

  def submitGrsData(
    businessType: BusinessType,
    journeyId: JourneyId
  ): Action[AnyContent] = actions
    .Applicant
    .authorised
    .ensureValidForm(
      form = form(businessType),
      viewToServeWhenFormHasErrors =
        implicit request =>
          view(
            _,
            businessType,
            journeyId
          )
    )
    .async:
      implicit request: (AuthorisedRequest[AnyContent] & FormValue[JourneyData]) =>
        val journeyData: JourneyData = request.formValue
        val json: JsValue = Json.toJson(journeyData)

        for {
          _ <- storeStubsData(businessType, journeyData)
        } yield {
          Redirect(AppRoutes.apply.internal.GrsController.journeyCallback(Some(journeyId)))
            .addingToSession(journeyId.value -> json.toString)
        }

  private def storeStubsData(
    businessType: BusinessType,
    journeyData: JourneyData
  )(using Request[?]): Future[Unit] =
    val soleTraderIndividualRecord =
      (businessType, journeyData.nino) match {
        case (SoleTrader, Some(nino: Nino)) => agentsExternalStubsConnector.storeIndividualUserRecord(nino, Seq("HMRC-MTD-IT"))
        case _ => Future.successful(())
      }

    val utr = journeyData.sautr.map(_.value).getOrElse(journeyData.ctutr.map(_.value).getOrElse(""))
    // to get BPR fetches working we need to store a BPR in stubs using GRS data
    val businessPartnerRecord = agentsExternalStubsConnector.storeBusinessPartnerRecord(
      BusinessPartnerRecord(
        businessPartnerExists = true,
        uniqueTaxReference = Some(utr),
        utr = Some(utr),
        safeId = journeyData.registration.registeredBusinessPartnerId.map(_.value).getOrElse(""),
        isAnIndividual = businessType === SoleTrader,
        individual =
          if businessType === SoleTrader then
            Some(
              uk.gov.hmrc.agentregistrationfrontend.testonly.model.Individual(
                firstName = journeyData.fullName.map(_.firstName).getOrElse("Test"),
                lastName = journeyData.fullName.map(_.lastName).getOrElse("User"),
                dateOfBirth = journeyData.dateOfBirth.map(_.toString).getOrElse("1990-01-01")
              )
            )
          else None,
        organisation =
          businessType match
            case BusinessType.Partnership.LimitedLiabilityPartnership | BusinessType.LimitedCompany | BusinessType.Partnership.LimitedPartnership | BusinessType.Partnership.ScottishLimitedPartnership =>
              Some(Organisation(
                organisationName = journeyData.companyProfile.map(_.companyName).getOrElse("BPR Test Org"),
                organisationType = "5T"
              ))
            case _ => None,
        addressDetails = AddressDetails(
          addressLine1 = "1 Test Street",
          addressLine2 = Some("Test Area"),
          addressLine3 = None,
          addressLine4 = None,
          postalCode = journeyData.postcode.getOrElse("TE1 1ST"),
          countryCode = "GB"
        ),
        contactDetails = Some(ContactDetails(
          primaryPhoneNumber = Some("01234567890"),
          emailAddress = Some("test@example.com")
        ))
      )
    )

    for {
      _ <- soleTraderIndividualRecord
      _ <- businessPartnerRecord
    } yield ()

  def retrieveGrsData(journeyId: JourneyId): Action[AnyContent] = Action: request =>
    request.session.get(journeyId.value) match {
      case Some(data) => Ok(data)
      case None => NotFound
    }

  def setupGrsJourney(businessType: BusinessType): Action[JourneyConfig] =
    Action(parse.json[JourneyConfig]): (_: Request[JourneyConfig]) =>
      Created(Json.obj(
        "journeyStartUrl" -> routes.GrsStubController.showGrsData(businessType, randomJourneyId()).url // scalafix:ok DisableSyntax
      ))

  val seed = 123456
  val utrGenerator = SaUtrGenerator(seed) // for our test-only purposes we can use SaUtrGenerator to generate both SA UTRs and CT UTRs
  val ninoGenerator = NinoGenerator(seed)
  def randomSaUtr(): SaUtr = SaUtr(utrGenerator.nextSaUtr.utr)
  def randomCtUtr(): CtUtr = CtUtr(utrGenerator.nextSaUtr.utr)
  def randomJourneyId(): JourneyId = JourneyId(UUID.randomUUID().toString)
  def randomNino(): Nino = Nino(ninoGenerator.nextNino.nino)

  private def form(businessType: BusinessType): Form[JourneyData] =
    val registrationStatusMapping: FieldMapping[RegistrationStatus] = Forms.of(FormatterFactory.makeEnumFormatter[RegistrationStatus](
      errorMessageIfMissing = "Registration status required",
      errorMessageIfEnumError = "Registration status invalid"
    ))

    Form(mapping(
      "registrationStatus" -> registrationStatusMapping,
      "safeId" -> mandatoryIf(
        isEqual("registrationStatus", GrsRegistered.toString),
        nonEmptyText
      ),
      "firstName" -> mandatoryIf(
        _ => businessType === SoleTrader,
        nonEmptyText
      ),
      "lastName" -> mandatoryIf(
        _ => businessType === SoleTrader,
        nonEmptyText
      ),
      "dateOfBirth" -> mandatoryIf(
        _ => businessType === SoleTrader,
        nonEmptyText
      ),
      "nino" -> mandatoryIf(
        isEqual("trn", "").and(_ => businessType === SoleTrader),
        nonEmptyText
      ),
      "trn" -> mandatoryIf(
        isEqual("nino", "").and(_ => businessType === SoleTrader),
        nonEmptyText
      ),
      "sautr" -> mandatoryIf(
        _ =>
          Seq(
            SoleTrader,
            GeneralPartnership,
            LimitedLiabilityPartnership,
            LimitedPartnership,
            ScottishLimitedPartnership,
            ScottishPartnership
          ).contains(businessType),
        nonEmptyText
      ),
      "companyNumber" -> mandatoryIf(
        _ =>
          Seq(
            LimitedCompany,
            LimitedLiabilityPartnership,
            LimitedPartnership,
            ScottishLimitedPartnership
          ).contains(businessType),
        nonEmptyText
      ),
      "companyName" -> mandatoryIf(
        _ =>
          Seq(
            LimitedCompany,
            LimitedLiabilityPartnership,
            LimitedPartnership,
            ScottishLimitedPartnership
          ).contains(businessType),
        nonEmptyText
      ),
      "dateOfIncorporation" -> optional(nonEmptyText),
      "ctutr" -> mandatoryIf(
        _ => businessType === LimitedCompany,
        nonEmptyText
      ),
      "postcode" -> mandatoryIf(
        _ =>
          Seq(
            GeneralPartnership,
            LimitedLiabilityPartnership,
            LimitedPartnership,
            ScottishLimitedPartnership,
            ScottishPartnership
          ).contains(businessType),
        nonEmptyText
      )
    )(
      (
        status,
        safeId,
        firstName,
        lastName,
        dateOfBirth,
        nino,
        trn,
        sautr,
        companyNumber,
        companyName,
        dateOfIncorporation,
        ctutr,
        postcode
      ) =>
        JourneyData(
          identifiersMatch = if status === GrsNotCalled then false else true,
          registration = Registration(
            registrationStatus = status,
            registeredBusinessPartnerId = safeId.map(SafeId.apply)
          ),
          fullName = firstName.map(first => FullName(first, lastName.getOrElse(""))),
          dateOfBirth = dateOfBirth.map(LocalDate.parse),
          nino = nino.map(Nino.apply),
          trn = trn,
          sautr = sautr.map(SaUtr.apply),
          // address = ... when/if adding support for SoleTrader address, don't add it to the stub page and just hardcode it here when trn is defined
          companyProfile = companyNumber.map(crn =>
            CompanyProfile(
              companyNumber = Crn(crn),
              companyName = companyName.getOrElse(""),
              dateOfIncorporation = dateOfIncorporation.map(LocalDate.parse),
              unsanitisedCHROAddress = Some(ChroAddress(
                address_line_1 = Some("23 Great Portland Street"),
                address_line_2 = Some("London"),
                country = Some("United Kingdom"),
                postal_code = Some("W1 1AQ")
              ))
            )
          ),
          ctutr = ctutr.map(CtUtr.apply),
          postcode = postcode
        )
    )(response =>
      Some((
        response.registration.registrationStatus,
        response.registration.registeredBusinessPartnerId.map(_.value),
        response.fullName.map(_.firstName),
        response.fullName.map(_.lastName),
        response.dateOfBirth.map(_.toString),
        response.nino.map(_.value),
        response.trn,
        response.sautr.map(_.value),
        response.companyProfile.map(_.companyNumber.value),
        response.companyProfile.map(_.companyName),
        response.companyProfile.flatMap(_.dateOfIncorporation.map(_.toString)),
        response.ctutr.map(_.value),
        response.postcode
      ))
    ))

  private def formWithDefaults(businessType: BusinessType): Form[JourneyData] = form(businessType).fill(JourneyData(
    identifiersMatch = true,
    registration = Registration(
      registrationStatus = GrsRegistered,
      registeredBusinessPartnerId = Some(SafeId("XA0001234512345"))
    ),
    fullName = if businessType === SoleTrader then Some(FullName("Test", "User")) else None,
    dateOfBirth = if businessType === SoleTrader then Some(LocalDate.now().minusYears(20)) else None,
    nino = if businessType === SoleTrader then Some(randomNino()) else None,
    trn = None,
    sautr =
      if Seq(
          SoleTrader,
          GeneralPartnership,
          LimitedLiabilityPartnership,
          ScottishPartnership,
          LimitedPartnership,
          ScottishLimitedPartnership
        ).contains(businessType)
      then Some(randomSaUtr())
      else None,
    companyProfile =
      if Seq(
          LimitedCompany,
          LimitedLiabilityPartnership,
          LimitedPartnership,
          ScottishLimitedPartnership
        ).contains(businessType)
      then
        Some(CompanyProfile(
          companyNumber = Crn("12345678"),
          companyName = if businessType === LimitedCompany then "Test Company Ltd" else "Test Partnership",
          dateOfIncorporation = Some(LocalDate.now().minusYears(10)),
          unsanitisedCHROAddress = Some(ChroAddress(
            address_line_1 = Some("23 Great Portland Street"),
            address_line_2 = Some("London"),
            country = Some("United Kingdom"),
            postal_code = Some("W1 1AQ")
          ))
        ))
      else None,
    ctutr = if businessType === LimitedCompany then Some(randomCtUtr()) else None,
    postcode =
      if Seq(
          GeneralPartnership,
          LimitedLiabilityPartnership,
          LimitedPartnership,
          ScottishLimitedPartnership,
          ScottishPartnership
        ).contains(businessType)
      then Some("AA1 1AA")
      else None
  ))
