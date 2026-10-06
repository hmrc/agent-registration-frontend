/*
 * Copyright 2025 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.agentregistrationfrontend.testsupport

import uk.gov.hmrc.mongo.test.MongoSupport

/** An ISpec whose application uses a test database of its own, emptied before each test, instead of the service's database. */
trait MongoISpec
extends ISpec,
  MongoSupport:

  // the service prefix keeps it apart from a same-named spec's database in the sibling services, whose suites may run at the same time
  override protected def databaseName: String = s"test-fe-${getClass.getSimpleName}"

  override protected def configOverrides: Map[String, Any] = Map[String, Any]("mongodb.uri" -> mongoUri)

  override def beforeEach(): Unit =
    super.beforeEach()
    dropDatabase()
