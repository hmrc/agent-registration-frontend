# Creating individuals for a partnership

Part of [Individuals on an application](individuals.md).

Applies to [general partnerships](glossary.md#partnership) and [Scottish partnerships](glossary.md#partnership):
`AgentApplicationGeneralPartnership` and `AgentApplicationScottishPartnership`.

```mermaid
flowchart TD
    Q0["NumberOfKeyIndividualsController<br/>Applicant: how many partners?"]
    N{"Answer"}
    A1["numberOfIndividuals =<br/>FiveOrLess(n)"]
    Z{"n = 0?"}
    A0["hasOtherRelevantIndividuals = Some(true)"]
    M0["MandatoryRelevantIndividualsController<br/>at least 1 IndividualProvidedDetails<br/>isPersonOfControl = false"]
    Q2["Applicant: how many partners are<br/>responsible for tax matters? (n)"]
    A2["numberOfIndividuals =<br/>SixOrMore(n)"]
    L{"Fewer IndividualProvidedDetails with<br/>isPersonOfControl = true than totalListSize?"}
    TY["EnterKeyIndividualController<br/>applicant types a partner name"]
    R1["Create 1 IndividualProvidedDetails<br/>individualName = typed name<br/>isPersonOfControl = true"]
    ORI{"Applicant: any other<br/>relevant individuals?"}
    ORY["hasOtherRelevantIndividuals = Some(true)"]
    ORY2["1 IndividualProvidedDetails per typed name<br/>isPersonOfControl = false"]
    ORN["hasOtherRelevantIndividuals = Some(false)"]
    ORN2["delete IndividualProvidedDetails<br/>with isPersonOfControl = false"]
    DONE["Both lists complete"]
    Q0 --> N
    N -->|5 or fewer| A1 --> Z
    Z -->|Yes| A0 --> M0 --> DONE
    Z -->|No| L
    N -->|6 or more| Q2 --> A2 --> L
    L -->|Yes| TY --> R1 --> L
    L -->|No| ORI
    ORI -->|Yes| ORY --> ORY2 --> DONE
    ORI -->|No| ORN --> ORN2 --> DONE
```
