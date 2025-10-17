# Task List Navigation Hub

## Quick Overview

Central navigation hub displaying application progress with GRS verification enforcement.

## Session State Model

```mermaid
graph TD
    A[Session State] --> B[AgentApplication]
    B --> C[UTR: String]
    B --> D[Business Details]
    B --> E[Applicant Details] 
    B --> F[AML Details]
    
    C --> G{UTR Present?}
    G -->|Yes| H[Access Granted]
    G -->|No| I[Redirect to GRS]
    
    D --> J[Agent Type<br/>Business Type<br/>Partnership Type]
    E --> K[Name<br/>LLP Role<br/>Contact Info]
    F --> L[Supervisor<br/>Registration<br/>Evidence Upload]
    
    style C fill:#e1f5fe
    style G fill:#fff3e0
    style H fill:#c8e6c9
    style I fill:#ffcdd2
```

## User Journey Sequence

```mermaid
sequenceDiagram
    participant U as User
    participant B as Browser
    participant C as TaskListController
    participant S as Session
    participant V as View

    U->>B: Navigate to /apply/task-list
    B->>C: GET request
    
    Note over C: Validate Session & UTR
    C->>S: Check application exists
    S-->>C: AgentApplication
    
    alt UTR Missing
        C->>B: Redirect to start registration
        B->>U: Show GRS flow
    else UTR Present
        Note over C: Calculate Section Status
        C->>C: Check business details completion
        C->>C: Check applicant details completion  
        C->>C: Check AML details completion
        
        C->>V: Render task list
        V-->>C: Task list HTML
        C->>B: 200 OK with task list
        B->>U: Display progress & navigation
    end
    
    U->>B: Click section link
    B->>U: Navigate to section
```

## Session Data Flow

```mermaid
graph LR
    A[Previous Page] --> B[Session Storage]
    B --> C[TaskListController]
    C --> D[Progress Calculation]
    D --> E[View Rendering]
    E --> F[User Navigation]
    
    subgraph "Session Contents"
        G[AgentApplication ID]
        H[User Authentication]
        I[Application State]
    end
    
    B --> G
    B --> H
    B --> I
    
    style B fill:#e3f2fd
    style D fill:#f3e5f5
    style E fill:#e8f5e8
```

## Technical Architecture

### Section Status Logic

```mermaid
flowchart TD
    A[AgentApplication] --> B{Business Details}
    A --> C{Applicant Details}
    A --> D{AML Details}
    
    B --> E[Agent Type COMPLETE]
    B --> F[Business Type COMPLETE]
    B --> G[GRS UTR COMPLETE]
    
    C --> H[Name Details]
    C --> I[LLP Member Info]
    C --> J[Contact Details]
    
    D --> K[Supervisor]
    D --> L[Registration Number]
    D --> M[Evidence Upload]
    
    E --> N[Complete]
    F --> N
    G --> N
    
    H --> O[In Progress]
    I --> O
    J --> O
    
    K --> P[Not Started ⭕]
    L --> P
    M --> P
    
    style N fill:#c8e6c9
    style O fill:#fff3e0
    style P fill:#ffebee
```

## Security & Access Control

```mermaid
graph TD
    A[User Request] --> B{Session Valid?}
    B -->|No| C[Redirect to Auth]
    B -->|Yes| D{Application Exists?}
    D -->|No| E[Redirect to GRS]
    D -->|Yes| F{UTR Present?}
    F -->|No| G[Access Denied]
    F -->|Yes| H[Show Task List]
    
    style C fill:#ffcdd2
    style E fill:#ffcdd2
    style G fill:#ffcdd2
    style H fill:#c8e6c9
```

## Error Scenarios

```mermaid
graph LR
    A[Error Types] --> B[Missing GRS]
    A --> C[Invalid Session]
    A --> D[Navigation Block]
    
    B --> E[Log Warning<br/>Redirect to Start]
    C --> F[Clean Restart<br/>New Session]
    D --> G[Show Requirements<br/>Guide User]
    
    style B fill:#ffebee
    style C fill:#ffebee
    style D fill:#fff3e0
```

## Performance Profile

| Metric | Value | Notes |
|--------|-------|-------|
| Response Time | < 100ms | Minimal processing |
| Template Render | < 50ms | Static + dynamic data |
| DB Queries | 1 | Session application only |
| Memory Usage | Low | Stateless operation |
| Scalability | High | No server state |
