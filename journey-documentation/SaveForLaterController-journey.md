# SaveForLaterController Journey

## Overview

Confirmation page showing users their application progress has been saved with guidance for returning.

## Session Data

```mermaid
graph LR
    A[Before] --> B[After Display]
    
    subgraph "Session Changes"
        C[Existing Application Data] --> D[Same Application Data]
    end
    
    A --> C
    B --> D
    
    style C fill:#e8f5e8
    style D fill:#e8f5e8
```

**Stored Data:**

- No session changes - read-only operation

## User Flow

```mermaid
sequenceDiagram
    participant U as User
    participant C as Controller
    participant S as Session
    
    U->>C: GET /save-and-come-back-later
    C->>S: Validate application exists
    S-->>C: Application data
    C-->>U: Show confirmation page
    
    Note over U: User sees save confirmation
    Note over U: User gets return instructions
    
    alt User continues
        U->>C: Click return to task list
        C-->>U: Redirect to task list
    else User exits
        U->>C: Sign out or exit
        C-->>U: Exit application
    end
```

## API Calls

**None** - Display-only page with no backend operations.

## Navigation Logic

| User Action | Next Step | Reason |
|-------------|-----------|--------|
| View confirmation | Display page | Show save confirmation |
| Return to task list | `/task-list` | Continue application |
| Sign out | Auth logout | Secure session end |
| Exit application | External site | Leave service |
