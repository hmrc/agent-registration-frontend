# GitHub Copilot Instructions for Agent Registration Frontend

## Project Overview
This is a Play Framework 3.0.9 frontend service built with Scala 3.6.1 for HMRC's agent registration system. The service handles agent applications for HMRC agent services accounts, supporting various business types (sole traders, limited companies, partnerships) with anti-money laundering (AML) compliance and file upload capabilities.

## Key Architecture Patterns

### Controller Structure
Controllers follow a strict pattern inheriting from `FrontendController(mcc, actions)`:
- Use `Actions.getApplicationInProgress` for session management
- Apply `.ensureValidFormAndRedirectIfSaveForLater()` for form validation
- Leverage `.async` for asynchronous operations with implicit `AgentApplicationRequest`
- Always update applications via `ApplicationService.updateApplication()`

Example controller action pattern:
```scala
def submit: Action[AnyContent] =
  actions.getApplicationInProgress
    .ensureValidFormAndRedirectIfSaveForLater(SomeForm.form, implicit r => view(_))
    .async:
      implicit request: (AgentApplicationRequest[AnyContent] & FormValue[FormType]) =>
        // Process form and update application
```

### Business Logic & State Management
- `AgentApplication` is the central domain model managed via `ApplicationService`
- Use QuickLens `.modify()` for immutable updates: `application.modify(_.field).setTo(newValue)`
- Shared models live in `uk.gov.hmrc.agentregistration.shared` package
- JSON configuration uses discriminator pattern in `JsonConfig.jsonConfiguration`

### Service Dependencies
Key integrations include:
- **GRS (Government Registration Service)**: For business verification across multiple entity types
- **Companies House**: For LLP member matching and company data
- **Upscan**: For secure file uploads (AML evidence)
- **Agent Registration Backend**: Main business logic service on port 22202

## Development Workflows

### SBT Commands
Essential development commands:
- `sbt runTestOnly` - Start with test-only routes enabled (includes test endpoints)
- `sbt relax` - Disable strict building (WartRemover, fatal warnings) for development
- `sbt strict` - Re-enable strict building for production-ready code
- `sbt clean test` - Run full test suite before commits

### Service Manager Integration
```bash
# Start all dependencies
sm2 --start AGENT_REGISTRATION_ALL && sm2 -s

# Stop all services  
sm2 -stop-all && sm2 -s
```

### Test Environment
- Integration tests extend `ISpec` with WireMock support
- Test data factory: `TdAll.tdAll` provides consistent test fixtures
- Stub services available in `testsupport.wiremock.stubs` package
- Test-only routes in `testOnlyDoNotUseInAppConf.routes` for debugging

## Project-Specific Conventions

### Route Structure
URLs follow RESTful hierarchy:
- `/agent-registration/apply/{section}/{page}` - Application flow
- `/agent-registration/apply/about-your-business/` - Business details
- `/agent-registration/apply/applicant/` - Contact details
- `/agent-registration/apply/anti-money-laundering/` - AML compliance

### Form Validation
- Forms use `FormatterFactory.makeEnumFormatter` for type-safe enum handling
- Error keys centralized in `ErrorKeys` object
- Welsh language support configurable via `features.welsh-language-support`

### File Upload Pattern
Upscan integration requires:
1. Initiate upload via `UpscanService`
2. Handle async callbacks with polling mechanism
3. Store upload details in application state
4. Maximum file size: 5MB (configurable)

### Configuration Management
- Microservice configs in `application.conf` under `microservice.services`
- Feature flags in `features` section (e.g., `grs-stub`, `welsh-language-support`)
- Service URLs built using `ServicesConfig.baseUrl()`

## Testing Patterns

### WireMock Stubs
Service stubs follow consistent pattern:
```scala
object ServiceStubs {
  def stubEndpoint(): StubMapping = StubMaker.make(
    httpMethod = POST,
    urlPattern = urlMatching("/api/endpoint"),
    responseStatus = OK,
    responseBody = Json.toJson(response).toString
  )
}
```

### Controller Testing
- Use `ControllerSpec` base class for controller tests
- Mock dependencies via `overridesModule` in test configuration
- Verify service calls with `StubMaker.verify()`

## Common Gotchas
- Always use absolute file paths for routes and configs
- Test routes MUST be prefixed with `/test-only/` in test route files
- Strict building mode affects local development - use `relax` command when needed
- GRS stub can be enabled via `features.grs-stub` for local testing without external GRS calls