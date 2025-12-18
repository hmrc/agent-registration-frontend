
# Overview agent-registration-frontend

TODO
This is a new service. No overview yet. 

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

```
 AWESOME_STUBS_FRONTEND                      [====================][100%] Done
 AWESOME_STUBS                               [====================][100%] Done
 INTERNAL_AUTH                               [====================][100%] Done
 ADDRESS_LOOKUP_FRONTEND                     [===========         ][ 59%] Install
 ADDRESS_LOOKUP                              [====================][100%] Starting
 ADDRESS_SEARCH_API                          [                    ][  0%] Pending
 AGENT_REGISTRATION                          [                    ][  0%] Pending
 AGENT_REGISTRATION_FRONTEND                 [                    ][  0%] Pending
 EMAIL                                       [                    ][  0%] Pending
 EMAIL_VERIFICATION_FRONTEND                 [                    ][  0%] Pending
 EMAIL_VERIFICATION                          [                    ][  0%] Pending
 EMAIL_VERIFICATION_STUBS                    [                    ][  0%] Pending
 HMRC_EMAIL_RENDERER                         [                    ][  0%] Pending
 PARTNERSHIP_IDENTIFICATION                  [                    ][  0%] Pending
 PARTNERSHIP_IDENTIFICATION_FRONTEND         [                    ][  0%] Pending
 SOLE_TRADER_IDENTIFICATION                  [                    ][  0%] Pending
 SOLE_TRADER_IDENTIFICATION_FRONTEND         [                    ][  0%] Pending
 INCORPORATED_ENTITY_IDENTIFICATION_FRONTEND [                    ][  0%] Pending
 INCORPORATED_ENTITY_IDENTIFICATION          [                    ][  0%] Pending
 TRACKING_CONSENT_FRONTEND                   [                    ][  0%] Pending
 UPSCAN_STUB                                 [                    ][  0%] Pending
```
```
+---------+-------+------------------------------------------------------------------+
| PID     | Port  | Reserved by                                                      |
+---------+-------+------------------------------------------------------------------+
| 279696  | 8100  | DATASTREAM                                                       |
| 279696  | 9984  | USERS_GROUPS_SEARCH                                              |
| 279696  | 8899  | FILE_UPLOAD_FRONTEND                                             |
| 279696  | 8500  | AUTH                                                             |
| 279696  | 7775  | ENROLMENT_STORE_PROXY_AM_JENKINS                                 |
| 279696  | 9967  | PERSONAL_DETAILS_VALIDATION                                      |
| 279696  | 9974  | SSO                                                              |
| 279571  | 9938  | IDENTITY_VERIFICATION_FRONTEND                                   |
| 279696  | 8898  | FILE_UPLOAD                                                      |
| 279571  | 9553  | BAS_GATEWAY_FRONTEND                                             |
| 279696  | 9978  | USER_DETAILS                                                     |
| 279696  | 9337  | CITIZEN_DETAILS                                                  |
| 279696  | 9927  | IDENTITY_VERIFICATION                                            |
| 279696  | 9995  | TAX_ENROLMENTS_DDCN                                              |
| 279571  | 9949  | AUTH_LOGIN_STUB                                                  |
| 279571  | 9041  | STRIDE_AUTH_FRONTEND                                             |
| 279571  | 9968  | PERSONAL_DETAILS_VALIDATION_FRONTEND                             |
| 279696  | 9904  | NINO_INSIGHTS_PROXY                                              |
| 279696  | 9991  | COMPANIES_HOUSE_API_PROXY                                        |
+---------+-------+------------------------------------------------------------------+

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

# Project Setup in IntelliJ

When importing a project into IntelliJ IDEA, it is recommended to configure your setup as follows to optimize the development process:

1. **SBT Shell Integration**: Utilize the sbt shell for project reloads and builds. This integration automates project discovery and reduces issues when running individual tests from the IDE.

2. **Enable Debugging**: Ensure that the "Enable debugging" option is selected. This allows you to set breakpoints and use the debugger to troubleshoot and fine-tune your code.

3. **Library and SBT Sources**: For those working on SBT project definitions, make sure to include "library sources" and "sbt sources." These settings enhance code navigation and comprehension by providing access to the underlying SBT and library code.

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

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").