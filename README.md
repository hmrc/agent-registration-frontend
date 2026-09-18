# Overview agent-registration-frontend

The user-facing part of agent registration. A business applies to become an HMRC agent here; the application itself is
stored by the agent-registration backend, and agent-registration-risking decides the outcome.

Two journeys live in this service.

The applicant journey (`controllers/applicant`) is for the person registering the business. It starts by identifying
the business through GRS, then works through a task list: agent details, contact details, the HMRC standard for agents,
AMLS supervision details with evidence uploaded via upscan, the list of individuals, and the declaration. The
application is saved after every page, so the applicant can leave and come back.

The list of individuals is the part with the most logic. Every person who runs the business has to be named and has to
provide their own details. How many are needed depends on the business type: a sole trader has one, an incorporated
business takes its officers from Companies House, and a partnership relies on numbers the applicant declares. See
individuals.md.

The individual journey (`controllers/individual`) is for those named people. The applicant shares one link; each person
signs in, is matched to the record held for them (by Citizen Details where possible, by name otherwise), and provides
their date of birth, National Insurance number, SA UTR, phone number and verified email. The applicant can also provide
someone else's details on their behalf. Once everyone has finished, the applicant declares and submits the
application, and the individual journey then shows risking progress and the outcome.

Test-only controllers under `testonly` (a GRS stub, fast-forward links into any journey state) are wired only through
testOnlyDoNotUseInAppConf.routes and exist to make local testing possible.

## Documentation

- [Individuals on an application](docs/individuals.md) - how key individuals and other relevant individuals are created,
  with a page per business type: [sole trader](docs/individuals-sole-trader.md),
  [incorporated business](docs/individuals-incorporated.md), [partnership](docs/individuals-partnership.md)
- [Glossary](docs/glossary.md) - domain terms used across these documents
- [Individual journey](docs/individual-journey.md) - how an individual signs in and is matched to their record
- [AMLS evidence upload](docs/upscan-mermaid.md) - the upscan flow for AMLS evidence files

# Running the Service

To start the service, use the following commands:

- `sbt runTestOnly` - this enables extra test endpoints
- `sbt run` to launch the service normally.

After starting the service, open in browser:
[`http://localhost:22201/agent-registration](http://localhost:22201/agent-registration)

Ensure that all dependent applications, including MongoDB and other microservices, are also running.
To start/stop these dependent services, use the Service Manager commands:

```bash
sm2 --start AGENT_REGISTRATION_ALL \
&& sm2 -s
```
```bash
sm2 -stop-all \
&& sm2 -s
```

In addition, if you are running this service from source and intending to upload AMLS evidence files in localhost then
you may also need to initialise an internal auth token used for object-store transfers by running the following command:

```bash
./setup-object-store.sh
```

## Using the GRS stub and company numbers

We have a stub (which is configured to be on in localhost by default) for the GRS service which allows us to specify
outcome data including Company Registration Numbers for incorporated business types. If we want to access predefined
company officer lists we can use company numbers from the following table:

| Company Type                  | Number of active officers | Company Number |
|-------------------------------|---------------------------|----------------|
| Limited Company               | 2                         | 11111111       |
| Limited Company               | 6                         | 11111116       |
| Limited Liability Partnership | 2                         | 22222222       |
| Limited Liability Partnership | 6                         | 22222226       |
| Limited Partnership           | 2                         | 33333333       |
| Limited Partnership           | 6                         | 33333336       |
| Scottish Limited Partnership  | 2                         | 44444444       |
| Scottish Limited Partnership  | 6                         | 44444446       |
| Any incorporated type         | 0                         | 55555555       |

# Project Setup in IntelliJ

When importing a project into IntelliJ IDEA, it is recommended to configure your setup as follows to optimize the
development process:

1. **SBT Shell Integration**: Utilize the sbt shell for project reloads and builds. This integration automates project
   discovery and reduces issues when running individual tests from the IDE.

2. **Enable Debugging**: Ensure that the "Enable debugging" option is selected. This allows you to set breakpoints and
   use the debugger to troubleshoot and fine-tune your code.

3. **Library and SBT Sources**: For those working on SBT project definitions, make sure to include "library sources"
   and "sbt sources." These settings enhance code navigation and comprehension by providing access to the underlying SBT
   and library code.

Here is a visual guide to assist you in setting up:
![img.png](readme/intellij-sbt-setup.png)

## Project specific sbt commands

### Turn off strict building

In sbt command in intellij:

```
sbt> relax
```

This will turn off strict building for this sbt session.
When you restart it, or you build on jenkins, this will be turned on.

### Run with test only endpoints

```
sbt> runTestOnly
```

### Run tests before check in

```
sbt> clean test
```

### License

This code is open source software licensed under
the [Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0.html).