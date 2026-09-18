# Individuals on an application

## The problem

HMRC checks the people behind a business before it approves an [agent application](glossary.md#agent-application). When
the application is submitted for risking, every person on the application is sent along with it. So the application has
to provide a list of those people, and each of them has to provide their own details.

There are two kinds of people on the list:

- [Key individuals](glossary.md#key-individual) - represented by `IndividualProvidedDetails(isPersonOfControl = true)`
- [Other relevant individuals](glossary.md#other-relevant-individual) - represented by
  `IndividualProvidedDetails(isPersonOfControl = false)`

The app has to work out **how many individuals of each type there are, and precreate records for them.** This differs
per business type.

## The algorithm

- [Sole trader](individuals-sole-trader.md) - `AgentApplicationSoleTrader`.
- [Incorporated business](individuals-incorporated.md) - `AgentApplicationLimitedCompany`, `AgentApplicationLlp`,
  `AgentApplicationLimitedPartnership`, `AgentApplicationScottishLimitedPartnership`.
- [Partnership](individuals-partnership.md) - `AgentApplicationGeneralPartnership`,
  `AgentApplicationScottishPartnership`.

The last two steps are the same for all of them.

### Adding other relevant individuals

- The [applicant](glossary.md#applicant) answers whether there are any, which sets `hasOtherRelevantIndividuals` to
  `Some(true)` or `Some(false)`. When there are 0 [key individuals](glossary.md#key-individual) the answer is already
  yes, and adding at least one is mandatory. Answering no deletes any existing [other relevant
  individual](glossary.md#other-relevant-individual) records.
- The [applicant](glossary.md#applicant) types their names one by one, and each name creates a record with
  `isPersonOfControl = false`.

### Providing details

Each person on the list provides their own details, or the [applicant](glossary.md#applicant) does it for them. The
application can be declared and submitted once everyone has.

- Every record starts with `providedDetailsState = Precreated`, and moves to `AccessConfirmed` when the
  [link](glossary.md#link) is shared, `Started` when the person signs in, and `Finished` when their details are in.
- `providedByApplicant` is `Some(false)` when the person provided their own details, `Some(true)` when the
  [applicant](glossary.md#applicant) did.
