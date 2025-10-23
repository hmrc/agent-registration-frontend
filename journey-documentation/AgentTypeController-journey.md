# AgentTypeController Journey

## Overview

First critical decision point determining UK vs Non-UK agent path.

## Session Data

```mermaid
graph LR
    A[Before] --> B[After Selection]
    
    subgraph "Session Changes"
        C[agentType: None] --> D[agentType: UkTaxAgent/NonUkTaxAgent]
    end
    
    A --> C
    B --> D
    
    style C fill:#ffebee
    style D fill:#e8f5e8
```

**Stored Data:**

- `agentType: AgentType` (UkTaxAgent | NonUkTaxAgent)

## User Flow

```mermaid
sequenceDiagram
    participant U as User
    participant C as Controller
    participant S as Session
    
    U->>C: GET /agent-type
    C->>S: Read existing agentType
    S-->>C: agentType (if exists)
    C-->>U: Show form (pre-filled if data exists)
    
    U->>C: POST /agent-type (selection)
    C->>C: Validate selection
    C->>S: Store agentType
    
    alt UK Agent
        C-->>U: Redirect to /business-type
    else Non-UK Agent
        C-->>U: Redirect to /exit
    end
```

## API Calls

**None** - No downstream API calls. Session-only operations.

## Navigation Logic

| Selection | Next Step | Reason |
|-----------|-----------|--------|
| UkTaxAgent | `/business-type` | Continue UK registration flow |
| NonUkTaxAgent | `/exit` | Non-UK agents not supported |

## Key Business Rules

- UK agents proceed to full registration
- Non-UK agents are directed to exit (service limitation)
- Selection stored in session for task list progress tracking
