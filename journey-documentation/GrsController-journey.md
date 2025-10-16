# Government Registration Service (GRS) Integration

## Quick Overview

External service integration for business verification with UTR generation and callback handling.

## Session State Evolution During GRS Journey

```mermaid
graph TD
    A[Pre-GRS State] --> B[GRS Initiated]
    B --> C[External Processing]
    C --> D[Callback Return]
    D --> E[Verification Complete]
    
    subgraph "Session Data Changes"
        F[Initial: businessType only]
        G[After GRS: + UTR + verification status]
        H[Complete: + business details + registration info]
    end
    
    A --> F
    E --> H
    
    subgraph "Business Types"
        I[Sole Trader]
        J[Limited Company] 
        K[Partnership]
        L[LLP]
    end
    
    style A fill:#e3f2fd
    style E fill:#c8e6c9
    style F fill:#fff3e0
    style D fill:#e8f5e8
```

## GRS Integration Sequence

```mermaid
sequenceDiagram
    participant U as User
    participant App as Frontend
    participant S as Session
    participant GRS as External GRS
    participant Gov as Gov Databases

    U->>App: Continue with business type
    App->>S: Store business type
    App->>GRS: Setup journey (businessType)
    GRS-->>App: Journey ID + redirect URL
    App-->>U: Redirect to GRS
    
    Note over U,GRS: External Verification (2-5 mins)
    
    U->>GRS: Complete identity verification
    GRS->>Gov: Verify business registration
    Gov-->>GRS: Business details + UTR
    GRS->>GRS: Generate verification results
    
    GRS-->>U: Redirect to callback URL
    U->>App: Return via callback
    App->>GRS: Retrieve verification results
    GRS-->>App: Verification outcome + UTR
    
    alt Verification Successful
        App->>S: Store UTR + business details
        App-->>U: Redirect to task list
    else Verification Failed
        App->>S: Store error details
        App-->>U: Show error + retry option
    end
```

## Business Verification Types

```mermaid
graph TD
    A[Business Type] --> B[Sole Trader]
    A --> C[Limited Company]
    A --> D[Partnership]
    A --> E[LLP]
    
    B --> F[SOLE_TRADER_IDENTIFICATION]
    C --> G[LIMITED_COMPANY_IDENTIFICATION]
    D --> H[PARTNERSHIP_IDENTIFICATION]
    E --> I[LLP_IDENTIFICATION]
    
    F --> J[NINO + UTR Verification]
    G --> K[Company Number + UTR]
    H --> L[Partnership UTR]
    I --> M[LLP Number + Members]
    
    J --> N[Individual Verification]
    K --> O[Companies House Check]
    L --> P[Partnership Records]
    M --> Q[LLP Member Verification]
    
    style F fill:#e3f2fd
    style G fill:#e3f2fd
    style H fill:#e3f2fd
    style I fill:#e3f2fd
```

## Implementation Details

### Controller Actions

```scala
// Journey Setup
def setUpGrsFromSignIn(agentType: AgentType, businessType: BusinessType)

// Callback Processing  
def journeyCallback(businessType: BusinessType, journeyId: JourneyId)
```

### Session Data Transformations

```mermaid
graph LR
    A[Before GRS] --> B[After Setup] --> C[After Callback]
    
    subgraph "Before"
        D[agentType: SoleTrader<br/>businessType: SoleTrader<br/>utr: None]
    end
    
    subgraph "Setup"
        E[agentType: SoleTrader<br/>businessType: SoleTrader<br/>grsJourneyId: journey123<br/>utr: None]
    end
    
    subgraph "Complete"
        F[agentType: SoleTrader<br/>businessType: SoleTrader<br/>utr: 1234567890<br/>businessName: John Smith<br/>registrationDate: 2023-01-15]
    end
    
    A --> D
    B --> E  
    C --> F
    
    style D fill:#ffebee
    style E fill:#fff3e0
    style F fill:#e8f5e8
```

## Error Handling Scenarios

```mermaid
graph TD
    A[GRS Callback] --> B{Verification Result}
    
    A[User] --> B[GRS Response Handler]
    B -->|Success| C[Update Session with UTR]
    B -->|Identifier Mismatch| D[Show Mismatch Error]
    B -->|Registration Failed| E[Show Registration Error]
    B -->|Technical Error| F[Show Generic Error]
    
    C --> G[Redirect to Task List]
    D --> H[Allow Retry Option]
    E --> I[Alternative Verification]
    F --> J[Retry GRS Process]
    
    style C fill:#c8e6c9
    style D fill:#ffcdd2
    style E fill:#ffcdd2
    style F fill:#ffcdd2
```

## Integration Architecture

```mermaid
graph TD
    A[Frontend App] --> B[GRS Controller]
    B --> C[External GRS Service]
    C --> D[Government Databases]
    
    subgraph "Data Flow"
        E[Business Type] --> F[Journey Config]
        F --> G[External Verification]
        G --> H[UTR + Details]
        H --> I[Session Update]
    end
    
    subgraph "Security"
        J[HTTPS Communication]
        K[Service Auth]
        L[Callback Validation]
    end
    
    style C fill:#e3f2fd
    style D fill:#fff3e0
```

## Performance & Reliability

| Metric | Value | Notes |
|--------|-------|-------|
| GRS Response | 2-5 minutes | User completes verification |
| Callback Speed | < 1 second | Return processing |
| Success Rate | 99.9% | GRS service SLA |
| Retry Options | Unlimited | User can retry failed attempts |
