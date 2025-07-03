/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.agentregistrationfrontend.pagesupport

import org.openqa.selenium.WebDriver

class Pages(baseUrl: String)(implicit webDriver: WebDriver) {

  private val bankTransferRelativeUrl: String = "bank-transfer"
  private val chequeRelativeUrl: String = "cheque"

  val startEndpoint: Endpoint = new Endpoint(baseUrl = baseUrl, path = "/get-an-income-tax-refund/start")

  val doYouWantToSignInPage = new DoYouWantToSignInPage(baseUrl = baseUrl)
  val doYouWantYourRefundViaBankTransferPage = new DoYouWantYourRefundViaBankTransferPage(baseUrl = baseUrl)

  val weNeedYouToConfirmYourIdentityBankTransferPage = new ConfirmYourIdentityPage(baseUrl            = baseUrl, pathForApplicationType = bankTransferRelativeUrl)
  val weNeedYouToConfirmYourIdentityChequePage = new ConfirmYourIdentityPage(baseUrl            = baseUrl, pathForApplicationType = chequeRelativeUrl)

  val whatIsYourP800ReferenceBankTransferPage = new EnterYourP800ReferencePage(baseUrl            = baseUrl, pathForApplicationType = bankTransferRelativeUrl)
  val whatIsYourP800ReferenceChequePage = new EnterYourP800ReferencePage(baseUrl            = baseUrl, pathForApplicationType = chequeRelativeUrl)

  val enterYourNationalInsuranceNumberBankTransferPage = new EnterYourNationalInsuranceNumberPage(baseUrl            = baseUrl, pathForApplicationType = bankTransferRelativeUrl)
  val enterYourNationalInsuranceNumberChequePage = new EnterYourNationalInsuranceNumberPage(baseUrl            = baseUrl, pathForApplicationType = chequeRelativeUrl)

  // bank transfer only
  val enterYourDateOfBirthPage = new EnterYourDateOfBirthPage(baseUrl = baseUrl)

  val checkYourAnswersBankTransferPage = new CheckYourAnswersPage(baseUrl            = baseUrl, pathForApplicationType = bankTransferRelativeUrl)
  val checkYourAnswersChequePage = new CheckYourAnswersPage(baseUrl            = baseUrl, pathForApplicationType = chequeRelativeUrl)

  val yourIdentityIsConfirmedBankTransferPage = new YourIdentityIsConfirmedPage(baseUrl            = baseUrl, pathForApplicationType = bankTransferRelativeUrl)
  val yourIdentityIsConfirmedChequePage = new YourIdentityIsConfirmedPage(baseUrl            = baseUrl, pathForApplicationType = chequeRelativeUrl)

  val cannotConfirmYourIdentityTryAgainBankTransferPage = new CannotConfirmYourIdentityTryAgainPage(baseUrl            = baseUrl, pathForApplicationType = bankTransferRelativeUrl)
  val cannotConfirmYourIdentityTryAgainChequePage = new CannotConfirmYourIdentityTryAgainPage(baseUrl            = baseUrl, pathForApplicationType = chequeRelativeUrl)

  val noMoreAttemptsLeftToConfirmYourIdentityBankTransferPage = new NoMoreAttemptsLeftToConfirmYourIdentityPage(baseUrl            = baseUrl, pathForApplicationType = bankTransferRelativeUrl)
  val noMoreAttemptsLeftToConfirmYourIdentityChequePage = new NoMoreAttemptsLeftToConfirmYourIdentityPage(baseUrl            = baseUrl, pathForApplicationType = chequeRelativeUrl)

  val refundRequestNotSubmittedPage = new RefundRequestNotSubmittedPage(baseUrl = baseUrl)
  val verifyingBankAccountPage = new VerifyingBankAccountPage(baseUrl       = baseUrl, consentStatus = ConsentStatus.Authorised, TdAll.tdAll.consentId, Some(TdAll.tdAll.bankReferenceId), "")
  def verifyingBankAccountPageConsent(consentStatusInput: ConsentStatus = ConsentStatus.Authorised, bankReferenceId: Option[BankReferenceId] = Some(TdAll.tdAll.bankReferenceId), consentId: ConsentId = TdAll.tdAll.consentId, failureCode: String = "") = new VerifyingBankAccountPage(baseUrl       = baseUrl, consentStatus = consentStatusInput, consentId, bankReferenceId, failureCode)

  //bank transfer specific page
  val chooseAnotherWayToReceiveYourRefundPage = new ChooseAnotherWayToReceiveYourRefundPage(baseUrl = baseUrl)
  //cheque specific page
  val claimYourRefundByBankTransferPage = new ClaimYourRefundByBankTransferPage(baseUrl = baseUrl)

  val giveYourConsentPage = new GiveYourConsentPage(baseUrl = baseUrl)

  val youCannotConfirmYourIdentityYetSpec = new YouCannotConfirmYourIdentityYetPage(baseUrl = baseUrl)

  val yourRefundRequestHasNotBeenSubmittedSpec = new YourRefundRequestHasNotBeenSubmittedPage(baseUrl       = baseUrl, consentStatus = ConsentStatus.Authorised, TdAll.tdAll.consentId, TdAll.tdAll.bankReferenceId)
  val yourRefundRequestHasNotBeenSubmittedSpecSelectOneAccount = new YourRefundRequestHasNotBeenSubmittedSelectOneAccountPage(baseUrl = baseUrl)
  val refundRequestNotSubmittedSelectDifferentBankAccount = new RefundRequestNotSubmittedSelectDifferentBankAccountPage(baseUrl = baseUrl)
  val refundCancelledSpec = new RefundCancelledPage(baseUrl = baseUrl)

  val thereIsAProblemPage = new ThereIsAProblemPage(baseUrl = baseUrl)

  def timeoutPage(didUserDelete: Boolean) = new TimeoutPage(baseUrl = baseUrl, didUserDelete)

  val isYourAddressUpToDate = new IsYourAddressUpToDatePage(baseUrl = baseUrl)
  val updateYourAddressPage = new UpdateYourAddressPage(baseUrl = baseUrl)

  val enterNameOfYourBankAccountPage = new EnterNameOfYourBankAccountPage(baseUrl = baseUrl)

  val requestReceivedBankTransferPage = new RequestReceivedPage(baseUrl            = baseUrl, pathForApplicationType = bankTransferRelativeUrl)
  val requestReceivedChequePage = new RequestReceivedPage(baseUrl            = baseUrl, pathForApplicationType = chequeRelativeUrl)

  // Page Stubs
  val govUkRouteInPage = new GovUkRouteInPage(baseUrl = baseUrl)
  val ptaSignInPage = new PtaSignInPage(baseUrl = baseUrl)
  val generalIncomeTaxEnquiriesPage = new GeneralIncomeTaxEnquiriesPage(baseUrl = baseUrl)
  val bankStubPage = new BankStubPage(baseUrl = baseUrl)
  val feedbackFrontendStubPageBankTransfer = new FeedbackFrontendStubPage(baseUrl            = baseUrl, pathForApplicationType = bankTransferRelativeUrl)
  val feedbackFrontendStubPageCheque = new FeedbackFrontendStubPage(baseUrl            = baseUrl, pathForApplicationType = chequeRelativeUrl)
}
