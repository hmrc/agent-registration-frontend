# Creating individuals for an incorporated business

Part of [Individuals on an application](individuals.md).

Applies to [incorporated businesses](glossary.md#incorporated-business): `AgentApplicationLimitedCompany`,
`AgentApplicationLlp`, `AgentApplicationLimitedPartnership` and `AgentApplicationScottishLimitedPartnership`. The
examples use `AgentApplicationLimitedCompany`; the other three change the same fields in the same way.

Before the task starts, after [GRS](glossary.md#grs):

```scala
AgentApplicationLimitedCompany(
  numberOfIndividuals = None,
  hasOtherRelevantIndividuals = None,
  ...
)
```

```mermaid
flowchart TD
    CH["CompaniesHouseOfficersController asks Companies House<br/>for the officers of this company (by getCrn)"]
    CH2["Companies House returns the active officers<br/>in the counted roles, with their names"]
    CH3["count = how many came back,<br/>after officers with invalid names are dropped"]
    N{"count"}
    A0["numberOfIndividuals =<br/>FiveOrLessOfficers(0, true)<br/>hasOtherRelevantIndividuals = Some(true)"]
    M0["MandatoryRelevantIndividualsController<br/>at least 1 IndividualProvidedDetails<br/>isPersonOfControl = false"]
    Q1{"Applicant: is the officer list correct?"}
    A1["numberOfIndividuals =<br/>FiveOrLessOfficers(count, true)"]
    R1["Create count IndividualProvidedDetails<br/>individualName = Companies House name<br/>isPersonOfControl = true"]
    A2["numberOfIndividuals =<br/>FiveOrLessOfficers(count, false)"]
    X2["Delete IndividualProvidedDetails with isPersonOfControl = true<br/>blocked until Companies House is updated"]
    Q2["Applicant: how many officers are<br/>responsible for tax matters? (n)"]
    A3["numberOfIndividuals =<br/>SixOrMoreOfficers(count, n)"]
    L{"Fewer than max(n, 5) IndividualProvidedDetails<br/>with isPersonOfControl = true?"}
    TY["Applicant types an officer name"]
    MT{"Matches an officer not yet listed?"}
    R3["Create 1 IndividualProvidedDetails<br/>individualName = Companies House name<br/>isPersonOfControl = true"]
    ER["Name not matched error"]
    ORI{"Applicant: any other<br/>relevant individuals?"}
    ORY["hasOtherRelevantIndividuals = Some(true)"]
    ORY2["1 IndividualProvidedDetails per typed name<br/>isPersonOfControl = false"]
    ORN["hasOtherRelevantIndividuals = Some(false)"]
    ORN2["delete IndividualProvidedDetails<br/>with isPersonOfControl = false"]
    DONE["Both lists complete"]
    CH --> CH2 --> CH3 --> N
    N -->|0| A0 --> M0 --> DONE
    N -->|1 to 5| Q1
    Q1 -->|Yes| A1 --> R1 --> ORI
    Q1 -->|No| A2 --> X2
    N -->|6 or more| Q2 --> A3 --> L
    L -->|Yes| TY --> MT
    MT -->|Yes| R3 --> L
    MT -->|No| ER --> TY
    L -->|No| ORI
    ORI -->|Yes| ORY --> ORY2 --> DONE
    ORI -->|No| ORN --> ORN2 --> DONE
```
