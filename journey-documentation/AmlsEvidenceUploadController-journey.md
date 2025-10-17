# AML Evidence Upload Journey

## Quick Overview

Secure document upload using Upscan with real-time progress tracking and virus scanning.

## Session State Evolution

```mermaid
graph TD
    A[Initial State] --> B[Upload Initiated]
    B --> C[File Processing]
    C --> D[Upload Complete]
    
    subgraph "Session Data"
        E[AgentApplication]
        F[AML Details]
        G[Upload Status]
    end
    
    A --> H[amlsEvidence: None]
    B --> I[amlsEvidence: InProgress<br/>reference: fileRef123]
    C --> J[amlsEvidence: InProgress<br/>reference: fileRef123<br/>polling: active]
    D --> K[amlsEvidence: Success<br/>reference: fileRef123<br/>url: secure-url<br/>fileName: evidence.pdf]
    
    style A fill:#e3f2fd
    style B fill:#fff3e0
    style C fill:#fff3e0
    style D fill:#c8e6c9
```

## Upload Sequence Diagram

```mermaid
sequenceDiagram
    participant U as User
    participant B as Browser
    participant C as Controller
    participant S as Session
    participant US as Upscan
    participant V as Virus Scanner

    U->>B: Navigate to upload page
    B->>C: GET /evidence
    C->>S: Get AML supervisor
    C->>US: Initialize upload session
    US-->>C: Upload URLs + file reference
    C->>S: Store file reference (InProgress)
    C-->>B: Upload form HTML
    B-->>U: Show upload form
    
    U->>B: Select file + submit
    B->>US: POST file (direct upload)
    US->>V: Scan file for viruses
    V-->>US: Scan result
    
    alt File Clean
        US->>C: Callback success
        C->>S: Update status (Success)
        US-->>B: Redirect to result page
    else Virus Found
        US->>C: Callback quarantine
        C->>S: Update status (Failed: QUARANTINE)
        US-->>B: Redirect to result page
    else Upload Failed
        US-->>B: Redirect to error page
        B-->>U: Show error details
    end
    
    B->>C: GET /upload-result
    C-->>B: Progress page with polling
    
    loop Status Polling
        B->>C: AJAX GET /status
        C->>S: Check upload status
        C-->>B: HTTP status code
        B->>B: Update UI
    end
```

## File Upload State Machine

```mermaid
stateDiagram-v2
    [*] --> NotStarted: Page Load
    NotStarted --> InProgress: File Selected & Uploaded
    InProgress --> Uploading: Upscan Processing
    Uploading --> VirusScanning: File Received
    
    VirusScanning --> Success: Clean File
    VirusScanning --> Quarantined: Virus Detected
    VirusScanning --> Failed: Scan Error
    
    InProgress --> Failed: Network Error
    Uploading --> Failed: Upload Error
    
    Success --> [*]: Continue Journey
    Failed --> NotStarted: Retry Upload
    Quarantined --> NotStarted: New File Required
    
    note right of Success: Session stores <br/>- File URL<br/>- File name<br/>- Success timestamp
    note right of Failed: Session stores <br/>- Error reason<br/>- Failure timestamp
    note right of Quarantined: Session stores <br/>- Quarantine reason<br/>- Security warning
```

## Technical Architecture

### Controller Actions Map

```mermaid
graph TD
    A[show] --> B[Initialize Upscan]
    A --> C[Store Reference]
    A --> D[Render Form]
    
    E[showError] --> F[Display Error Details]
    
    G[showResult] --> H[Progress Page]
    H --> I[Start Polling]
    
    J[pollResultWithJavaScript] --> K{Check Status}
    K -->|InProgress| L[204 No Content]
    K -->|Success| M[202 Accepted] 
    K -->|Failed| N[400 Bad Request]
    K -->|Quarantine| O[409 Conflict]
    
    style A fill:#e3f2fd
    style G fill:#e3f2fd
    style J fill:#fff3e0
```

### Session State Updates

```scala
// Initial state
.modify(_.amlsDetails.each.amlsEvidence)
.setTo(Some(UploadDetails(
  status = UploadStatus.InProgress,
  reference = fileReference
)))

// Success state  
.setTo(Some(UploadDetails(
  status = UploadStatus.UploadedSuccessfully(url, fileName),
  reference = fileReference
)))
```

## AJAX Polling Mechanism

```mermaid
sequenceDiagram
    participant JS as JavaScript
    participant C as Controller
    participant S as Session
    
    loop Every 2 seconds
        JS->>C: GET /status (with CORS)
        C->>S: Check upload status
        S-->>C: Current status
        
        alt Still Processing
            C-->>JS: 204 No Content
            JS->>JS: Show spinner
        else Upload Complete
            C-->>JS: 202 Accepted
            JS->>JS: Show success + redirect
        else File Quarantined
            C-->>JS: 409 Conflict
            JS->>JS: Show security error
        else Upload Failed
            C-->>JS: 400 Bad Request
            JS->>JS: Show retry option
        end
    end
```

## Security & File Validation

```mermaid
graph TD
    A[File Upload] --> B{Size Check}
    B -->|> 5MB| C[Too Large]
    B -->|≤ 5MB| D{Type Check}
    
    D -->|Invalid| E[Wrong Type]
    D -->|PDF/JPG/PNG| F[Virus Scan]
    
    F -->|Clean| G[Store Securely]
    F -->|Infected| H[Quarantine]
    F -->|Scan Failed| I[Processing Error]
    
    style C fill:#ffcdd2
    style E fill:#ffcdd2
    style H fill:#ff8a65
    style I fill:#ffcdd2
    style G fill:#c8e6c9
```

## Performance Profile

| Metric | Value | Notes |
|--------|-------|-------|
| Upload Speed | Variable | Network dependent |
| Virus Scan | 10-30s | Upscan processing |
| Polling Interval | 2s | Configurable |
| Max File Size | 5MB | Security limit |
| Timeout | 10min | Upload limit |

## Error Handling Matrix

| Error Type | HTTP Code | User Experience | Session Impact |
|------------|-----------|-----------------|----------------|
| File Too Large | 400 | Size guidance | No change |
| Wrong Type | 400 | Format list | No change |
| Virus Found | 409 | Security warning | Status: Failed |
| Network Fail | 400 | Retry option | Status: Failed |
| Service Down | 500 | Try later | No change |

- **204 No Content**: Upload still in progress
- **202 Accepted**: Upload completed successfully  
- **409 Conflict**: File quarantined (virus detected)
- **400 Bad Request**: Upload failed
