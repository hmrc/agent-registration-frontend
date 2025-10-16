# Agent Registration Frontend - Journey Documentation

This directory contains concise, visual documentation for user journeys in the agent-registration-frontend service. The documentation follows a streamlined approach focused on **session data changes**, **API calls**, and **user flow** while minimizing implementation details.

## Documentation Structure

### Concise Documentation (`*-journey.md`)

Human-readable journey documentation with embedded mermaid diagrams focusing on:

- **Session Data**: What gets stored/retrieved from session storage
- **API Calls**: Downstream service calls and integrations
- **User Flow**: Sequence of user actions and system responses  
- **Navigation Logic**: Conditional routing based on user choices

### JSON Definitions (`*-journey.json`)

Machine-readable journey definitions containing:

- Route mappings and user actions
- Form handling and validation rules
- Backend service interactions
- Session and application state management

### Visual Journey Overview

[VISUAL-JOURNEY-OVERVIEW.md](./VISUAL-JOURNEY-OVERVIEW.md) provides a comprehensive visual summary of:

- Complete user journey flow across all controllers
- Session data evolution throughout the application
- Cross-controller data dependencies
- Security and validation patterns
- Performance metrics and state transitions

## Available Journey Documentation

### Core Business Flow Controllers

#### AgentTypeController

- **Routes**: `GET/POST /apply/about-your-business/agent-type`
- **Purpose**: Critical business branch point determining UK vs Non-UK agent registration path
- **Session Data**: Stores `agentType` (Individual/Company)
- **API Calls**: None - Session-only operations
- **Files**:
  - [AgentTypeController-journey.md](./AgentTypeController-journey.md) *(concise format)*
  - [AgentTypeController-journey.json](./AgentTypeController-journey.json)

#### GrsController

- **Routes**: `/apply/business-verification/*`
- **Purpose**: Government Registration Service integration for business verification
- **Session Data**: Stores UTR, business details, verification status
- **API Calls**: External GRS API, Government databases
- **Files**:
  - [GrsController-journey.md](./GrsController-journey.md)
  - [GrsController-journey.json](./GrsController-journey.json)

#### TaskListController

- **Routes**: `GET /apply/task-list`
- **Purpose**: Central navigation hub displaying application progress
- **Session Data**: Read-only access to full application state
- **API Calls**: None - Navigation only
- **Files**:
  - [TaskListController-journey.md](./TaskListController-journey.md)
  - [TaskListController-journey.json](./TaskListController-journey.json)

#### AmlsEvidenceUploadController

- **Routes**: `/apply/anti-money-laundering/evidence`
- **Purpose**: Secure document upload using Upscan with virus scanning
- **Session Data**: Stores upload status, file references, completion state
- **API Calls**: Upscan service, virus scanning service
- **Files**:
  - [AmlsEvidenceUploadController-journey.md](./AmlsEvidenceUploadController-journey.md)
  - [AmlsEvidenceUploadController-journey.json](./AmlsEvidenceUploadController-journey.json)

#### SaveForLaterController

- **Routes**: `GET /apply/save-for-later`
- **Purpose**: Application progress persistence and session validation
- **Session Data**: Read-only validation of application state
- **API Calls**: None - Session operations only
- **Files**:
  - [SaveForLaterController-journey.md](./SaveForLaterController-journey.md)
  - [SaveForLaterController-journey.json](./SaveForLaterController-journey.json)

#### CompaniesHouseMatchingController

- **Routes**: `GET/POST /apply/applicant/member-name-match`
- **Purpose**: LLP member validation against Companies House officer records
- **Session Data**: Stores member validation results and LLP details
- **API Calls**: Companies House API
- **Files**:
  - [CompaniesHouseMatchingController-journey.json](./CompaniesHouseMatchingController-journey.json)
  - *Documentation file pending*

## Key Patterns Documented

### Concise Documentation Approach

- **Session Data Focus**: Clear visualization of what data gets stored/modified
- **API Call Mapping**: Explicit documentation of downstream service dependencies
- **User Flow Sequences**: Mermaid sequence diagrams showing user-system interactions
- **Navigation Tables**: Structured mapping of user actions to system responses

### Action Composition

- Custom action builders (`getApplicationInProgress`, `ensureValidForm`)
- Business rule validation in action chains
- Automatic error handling and redirects

### Backend Service Integration

- Companies House API integration patterns
- Government Registration Service (GRS) flows
- Upscan file upload patterns with virus scanning

### State Management

- Session-based user progress tracking
- Central `AgentApplication` domain model updates
- QuickLens immutable update patterns

### Form Handling

- Play Framework form validation
- Enum formatter usage with `FormatterFactory`
- Complex conditional form processing

## How to Use This Documentation

### For Developers

1. Start with [VISUAL-JOURNEY-OVERVIEW.md](./VISUAL-JOURNEY-OVERVIEW.md) for complete application flow
2. Review specific controller `*-journey.md` files for detailed behavior
3. Reference JSON files for precise technical implementation details
4. Check session data sections for state dependencies

### For AI Assistants

1. Always reference [VISUAL-JOURNEY-OVERVIEW.md](./VISUAL-JOURNEY-OVERVIEW.md) for context
2. Use `*-journey.md` files for controller-specific behavior
3. Check JSON files for structured technical information
4. Follow the documented session data patterns for consistency

## Documentation Maintenance

### When Adding New Controllers

1. Create `<ControllerName>-documentation.md` following the concise format:
   - **Overview**: One sentence describing purpose
   - **Session Data**: Mermaid diagram + stored data list
   - **User Flow**: Sequence diagram showing interactions
   - **API Calls**: Service name + purpose OR "None"
   - **Navigation Logic**: Table of user actions → system responses

2. Create `<ControllerName>-journey.json` with structured technical details

3. Update this README with new controller entry

4. Update [VISUAL-JOURNEY-OVERVIEW.md](./VISUAL-JOURNEY-OVERVIEW.md) if controller adds new:
   - Session data fields
   - Cross-controller dependencies  
   - User flow steps
   - API integrations

### Documentation Standards

- **No Emojis**: Use plain text headers and descriptions
- **Embedded Diagrams**: Include mermaid diagrams inline, not as separate files
- **Concise Content**: Focus on essential information only
- **Visual Priority**: Prefer diagrams and tables over prose explanations

## Contributing

When updating journey documentation:

1. Follow the established concise format
2. Update both controller documentation AND this README
3. Ensure [VISUAL-JOURNEY-OVERVIEW.md](./VISUAL-JOURNEY-OVERVIEW.md) reflects changes
4. Test all mermaid diagram syntax
5. Maintain consistency with existing documentation patterns

## Related Documentation

- [GitHub Copilot Instructions](../.github/copilot-instructions.md)
- [Frontend Mapping Generation Prompt](../frontend-mapping-generation-prompt.md)
- [Project README](../README.md)
- [Visual Journey Overview](./VISUAL-JOURNEY-OVERVIEW.md)
