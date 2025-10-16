# Agent Registration - Visual Journey Overview

## Complete User Journey Flow

```mermaid
graph TD
    A[Start Registration] --> B[Agent Type Selection]
    B --> C[Business Type Selection]
    C --> D[GRS Verification]
    D --> E[Task List Hub]
    E --> F[Applicant Details]
    F --> G[AML Evidence Upload]
    G --> H[Review & Submit]
    
    subgraph "Session State Evolution"
        I[Empty Session] --> J[Agent Type Stored]
        J --> K[Business Type + GRS Journey]
        K --> L[UTR + Business Details]
        L --> M[Full Application Data]
    end
    
    B --> J
    C --> K
    D --> L
        H --> I[Submit Application]
```

## Session Data Model Across Controllers

    style A fill:#e3f2fd
    style E fill:#fff3e0
    style H fill:#c8e6c9

```

## Session Data Model Across Controllers

```mermaid
graph TD
    A[AgentApplication Session] --> B[Basic Info]
    A --> C[Business Details]
    A --> D[Applicant Info]
    A --> E[AML Details]
    
    B --> B1[Agent Type: Individual/Company]
    B --> B2[Business Type: SoleTrader/LimitedCompany/Partnership/LLP]
    B --> B3[UTR: String - from GRS]
    
    C --> C1[Business Name: String]
    C --> C2[Registration Date: LocalDate]
    C --> C3[Company Number: Option String]
    
    D --> D1[Applicant Name: String]
    D --> D2[LLP Role: Option String]
    D --> D3[Contact Details: Phone/Email]
    
    E --> E1[Supervisor: AML Body]
    E --> E2[Registration Number: String]
    E --> E3[Evidence Upload: UploadDetails]
    
    style A fill:#e3f2fd
    style B fill:#fff3e0
    style C fill:#e8f5e8
    style D fill:#f3e5f5
    style E fill:#fce4ec
```

## Controller-Specific Session Changes

### AgentTypeController

```mermaid
graph LR
    A[Before: Empty] --> B[After: agentType set]
    
    subgraph "Session Update"
        C[agentType: Individual/Company<br/>timestamp: now()]
    end
    
    B --> C
    style A fill:#ffebee
    style B fill:#e8f5e8
```

### GrsController  

```mermaid
graph LR
    A[Before: businessType only] --> B[After: UTR + verification]
    
    subgraph "Session Update"
        C[utr: Some(1234567890)<br/>businessName: Verified Name<br/>registrationDate: 2023-01-15<br/>grsJourneyComplete: true]
    end
    
    B --> C
    style A fill:#fff3e0
    style B fill:#c8e6c9
```

### AmlsEvidenceUploadController

```mermaid
graph LR
    A[Before: AML details only] --> B[After: + upload status]
    
    subgraph "Session Update"
        C[amlsEvidence: UploadDetails(<br/>  status: UploadedSuccessfully,<br/>  reference: file-ref-123,<br/>  url: secure-download-url,<br/>  fileName: evidence.pdf<br/>)]
    end
    
    B --> C
    style A fill:#fce4ec
    style B fill:#c8e6c9
```

## Security & Validation Flow

```mermaid
graph TD
    A[Every Request] --> B{Session Valid?}
    B -->|No| C[Redirect to Auth]
    B -->|Yes| D{Application Exists?}
    D -->|No| E[Start New Application]
    D -->|Yes| F{Controller-Specific Checks}
    
    F --> G{TaskList: UTR Required}
    F --> H{Upload: AML Details Required}
    F --> I{GRS: Business Type Required}
    
    G -->|Pass| J[Show Task List]
    G -->|Fail| K[Redirect to GRS]
    
    H -->|Pass| L[Show Upload Form]
    H -->|Fail| M[Redirect to AML Setup]
    
    I -->|Pass| N[Process GRS]
    I -->|Fail| O[Show Error]
    
    style C fill:#ffcdd2
    style E fill:#fff3e0
    style J fill:#c8e6c9
    style L fill:#c8e6c9
    style N fill:#c8e6c9
```

## Data Flow Between Controllers

```mermaid
sequenceDiagram
    participant U as User
    participant AT as AgentType
    participant GRS as GrsController
    participant TL as TaskList
    participant UP as Upload
    participant S as Session

    U->>AT: Select agent type
    AT->>S: Store agentType
    
    U->>GRS: Business verification
    GRS->>S: Store UTR + business details
    
    U->>TL: View progress
    TL->>S: Read all application data
    TL-->>U: Show completion status
    
    U->>UP: Upload evidence
    UP->>S: Store upload details
    
    Note over S: Complete application ready for submission
```

## UI State Indicators

### Task List Progress Visualization

```mermaid
graph LR
    A[About Business] --> A1{Complete?}
    B[Applicant Details] --> B1{Complete?}
    C[AML Details] --> C1{Complete?}
    
    A1 -->|Yes| A2[Completed]
    A1 -->|Partial| A3[In Progress]
    A1 -->|No| A4[Not Started]
    
    B1 -->|Yes| B2[Completed]
    B1 -->|Partial| B3[In Progress]
    B1 -->|No| B4[Not Started]
    
    C1 -->|Yes| C2[Completed]
    C1 -->|Partial| C3[In Progress]
    C1 -->|No| C4[Not Started]
    
    style A2 fill:#c8e6c9
    style B2 fill:#c8e6c9
    style C2 fill:#c8e6c9
    style A3 fill:#fff3e0
    style B3 fill:#fff3e0
    style C3 fill:#fff3e0
    style A4 fill:#ffebee
    style B4 fill:#ffebee
    style C4 fill:#ffebee
```

## Responsive Design Patterns

### Mobile Journey Adaptation

```mermaid
graph TD
    A[Mobile User] --> B[Simplified Navigation]
    B --> C[One Section at a Time]
    C --> D[Progress Indicator]
    D --> E[Save for Later Prominent]
    
    F[Desktop User] --> G[Full Task List View]
    G --> H[Multiple Sections Visible]
    H --> I[Side Navigation]
    I --> J[Breadcrumb Trail]
    
    style A fill:#e3f2fd
    style F fill:#e3f2fd
```

## Debug & Monitoring Views

### Session State Inspector

```yaml
AgentApplication:
  id: "app-12345"
  userId: "user-67890"
  createdAt: "2023-10-16T09:00:00Z"
  lastUpdated: "2023-10-16T10:30:00Z"
  
  agentType: "Individual"
  businessType: "SoleTrader"
  utr: "1234567890"
  businessName: "John Smith Trading"
  
  applicantDetails:
    name: "John Smith"
    telephone: "+44 1234 567890"
    
  amlsDetails:
    supervisoryBody: "HMRC"
    registrationNumber: "AML123456"
    evidence:
      status: "UploadedSuccessfully"
      fileName: "aml-certificate.pdf"
      uploadedAt: "2023-10-16T10:15:00Z"
```

## Performance Metrics Dashboard

| Controller | Avg Response | Session Operations | External Calls |
|------------|-------------|-------------------|-----------------|
| AgentType | 45ms | 1 write | 0 |
| GrsController | 3.2s | 2 writes | 2 (GRS API) |
| TaskList | 35ms | 1 read | 0 |
| Upload | 85ms | 2 writes | 1 (Upscan) |
| CompaniesHouse | 1.8s | 1 write | 1 (CH API) |

## State Transition Map

```mermaid
stateDiagram-v2
    [*] --> Empty: User starts
    Empty --> AgentTypeSelected: POST /agent-type
    AgentTypeSelected --> BusinessTypeSet: POST /business-type
    BusinessTypeSet --> GrsInitiated: GET /grs-setup
    GrsInitiated --> GrsComplete: GRS callback
    GrsComplete --> TaskListAccess: UTR validated
    TaskListAccess --> ApplicantInProgress: Start applicant section
    ApplicantInProgress --> AmlInProgress: Start AML section
    AmlInProgress --> UploadInProgress: Start file upload
    UploadInProgress --> ApplicationComplete: All sections done
    ApplicationComplete --> Submitted: Final submission
    
    note right of GrsComplete: Session now contains UTR
    note right of UploadInProgress: Session contains file reference
    note right of ApplicationComplete: Ready for submission
```

This visual overview provides a comprehensive view of how session data evolves across the entire agent registration journey, making it easy to understand data flow and state management patterns.
