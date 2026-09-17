# Glossary

Domain terms used in the agent registration frontend documentation, in alphabetical order, explained in terms of the
code. See [Individuals on an application](individuals.md) for how they fit together.

## Agent application

An [`AgentApplication`](app/uk/gov/hmrc/agentregistration/shared/AgentApplication.scala): a sealed trait with one final
case class per [business type](#business-type), for example `AgentApplicationSoleTrader`, stored in the backend. It
holds the [applicant](#applicant)'s answers, including `numberOfIndividuals` and `hasOtherRelevantIndividuals`. Each
individual is a separate
[`IndividualProvidedDetails`](app/uk/gov/hmrc/agentregistration/shared/individual/IndividualProvidedDetails.scala)
record that points to it through `agentApplicationId`.

## Applicant

The signed-in user whose `internalUserId` is stored on the `AgentApplication`. The frontend loads their application with
`AgentRegistrationConnector.findApplication()`, which calls the backend's `GET /application`, looked up by internal user
id. How they relate to the business is `AgentApplication.userRole`.

## Business type

[`BusinessType`](app/uk/gov/hmrc/agentregistration/shared/BusinessType.scala), stored as
`AgentApplication.businessType`, and also expressed by which `AgentApplication` case class is used. For creating
individuals, the code groups the case classes with type aliases in the `AgentApplication` companion: `IsSoleTrader`,
`IsIncorporated` and `IsAgentApplicationForDeclaringNumberOfKeyIndividuals`. `BusinessType.Partnership` is not the same
grouping: it includes LLPs and limited partnerships, which count as [incorporated businesses](#incorporated-business)
here.

## Companies House officer

A [`CompaniesHouseOfficer`](app/uk/gov/hmrc/agentregistration/shared/companieshouse/CompaniesHouseOfficer.scala) kept by
`CompaniesHouseService.getActiveOfficers`: not resigned (`resignedOn` is empty), a natural person (`identification` is
empty), and in one of the roles `getCompaniesHouseOfficerRole` returns for the [business type](#business-type)
([`CompaniesHouseOfficerRole`](app/uk/gov/hmrc/agentregistration/shared/companieshouse/CompaniesHouseOfficerRole.scala)):

- Limited company: `Director`, `NomineeDirector`.
- LLP: `LlpMember`, `LlpDesignatedMember`.
- Limited partnership, Scottish limited partnership: `GeneralPartnerLimitedPartnership`,
  `LimitedPartnerLimitedPartnership`.

Their normalised names become `individualName` on [key individual](#key-individual) records. Names that fail
`IndividualName.isValidName` are skipped.

## GRS

Generic Registration Service: HMRC's journeys that identify a business (sole trader, incorporated entity and partnership
identification). The [applicant](#applicant) is sent to GRS, and
[`GrsController`](app/uk/gov/hmrc/agentregistrationfrontend/controllers/applicant/internal/GrsController.scala) reads
the result as [`JourneyData`](app/uk/gov/hmrc/agentregistrationfrontend/model/grs/JourneyData.scala) and saves it as
`AgentApplication.businessDetails`, for example `BusinessDetailsSoleTrader`, whose `fullName` names the [sole trader
owner](#owner).

## Incorporated business

An `AgentApplication.IsIncorporated`: `AgentApplicationLimitedCompany`, `AgentApplicationLlp`,
`AgentApplicationLimitedPartnership` or `AgentApplicationScottishLimitedPartnership`. Its `numberOfIndividuals` is an
`Option[NumberOfCompaniesHouseOfficers]`, and its [key individuals](#key-individual) come from [Companies House
officers](#companies-house-officer).

## Key individual

An [`IndividualProvidedDetails`](app/uk/gov/hmrc/agentregistration/shared/individual/IndividualProvidedDetails.scala)
record with `isPersonOfControl = true`. It stands for a person who runs the business: the [sole trader owner](#owner), a
[Companies House officer](#companies-house-officer), or a [partner](#partner). How many there should be is
`AgentApplication.numberOfIndividuals`.

## Link

The URL `/agent-registration/provide-details/start/:linkId`
([`StartController`](app/uk/gov/hmrc/agentregistrationfrontend/controllers/individual/StartController.scala)), where
`linkId` is `AgentApplication.linkId`. Every individual on the application gets the same link. When the
[applicant](#applicant) confirms they shared it,
[`LinkController`](app/uk/gov/hmrc/agentregistrationfrontend/controllers/applicant/listdetails/link/LinkController.scala)
moves every `Precreated` record to `AccessConfirmed`.

## Other relevant individual

An [`IndividualProvidedDetails`](app/uk/gov/hmrc/agentregistration/shared/individual/IndividualProvidedDetails.scala)
record with `isPersonOfControl = false`. It stands for anyone else involved in the business's tax matters who is not a
[key individual](#key-individual). Whether there are any is `AgentApplication.hasOtherRelevantIndividuals`. [Sole
traders](#sole-trader) don't have them.

## Owner

`UserRole.Owner` in `AgentApplication.userRole`, only possible for a [sole trader](#sole-trader)
(`AgentApplicationSoleTrader.isOwner`). The [applicant](#applicant) and the [sole trader](#sole-trader)'s single [key
individual](#key-individual) are the same person, so
[`ProveIdentityController`](app/uk/gov/hmrc/agentregistrationfrontend/controllers/applicant/listdetails/soletrader/ProveIdentityController.scala)
fills that `IndividualProvidedDetails` record from the application and no [link](#link) is shared.

## Partner

A [key individual](#key-individual) of a [partnership](#partnership): an `IndividualProvidedDetails` record with
`isPersonOfControl = true` on an `IsAgentApplicationForDeclaringNumberOfKeyIndividuals` application, typed in by the
[applicant](#applicant) in `EnterKeyIndividualController`. Only people with the title "partner" count, not partner
organisations. For limited partnerships the partners are [Companies House officers](#companies-house-officer) instead.
Not to be confused with `UserRole.Partner`, the [applicant](#applicant)'s own role.

## Partnership

An `AgentApplication.IsAgentApplicationForDeclaringNumberOfKeyIndividuals`: `AgentApplicationGeneralPartnership` or
`AgentApplicationScottishPartnership`. Its `numberOfIndividuals` is an `Option[NumberOfRequiredKeyIndividuals]`,
declared by the [applicant](#applicant) in `NumberOfKeyIndividualsController`. Not the same as
`BusinessType.Partnership`, which also includes LLPs and limited partnerships.

## Sole trader

An `AgentApplicationSoleTrader` (`AgentApplication.IsSoleTrader`, `BusinessType.SoleTrader`). Its `numberOfIndividuals`
is always `Some(FiveOrLess(numberOfKeyIndividuals = 1))`, computed by the class rather than stored. Its single [key
individual](#key-individual) record is created by the sole trader task, and `hasOtherRelevantIndividuals` stays `None`.
