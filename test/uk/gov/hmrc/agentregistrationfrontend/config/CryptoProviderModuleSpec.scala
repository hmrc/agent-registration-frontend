/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.agentregistrationfrontend.config

import uk.gov.hmrc.agentregistrationfrontend.testsupport.ISpec
import uk.gov.hmrc.crypto.Crypted
import uk.gov.hmrc.crypto.Decrypter
import uk.gov.hmrc.crypto.Encrypter
import uk.gov.hmrc.crypto.PlainText

class CryptoProviderModuleSpec
extends ISpec:

  private val fieldLevelEncryptionKey = "edkOOwt7uvzw1TXnFIN6aRVHkfWcgiOrbBvkEQvO65g="
  override def configOverrides: Map[String, Any] =
    super.configOverrides ++ Map(
      "fieldLevelEncryption.enable" -> true,
      "fieldLevelEncryption.key" -> fieldLevelEncryptionKey
    )

  def crypto: Encrypter & Decrypter = app.injector.instanceOf[Encrypter & Decrypter]

  "crypto instance should encrypt new values with AES-GCM from field level encryption config" in:
    val firstEncrypted: Crypted = crypto.encrypt(PlainText("user-answer"))
    val secondEncrypted: Crypted = crypto.encrypt(PlainText("user-answer"))
    firstEncrypted.value should not be "user-answer"
    secondEncrypted.value should not be "user-answer"
    firstEncrypted should not be secondEncrypted
    crypto.decrypt(firstEncrypted).value shouldBe "user-answer"
    crypto.decrypt(secondEncrypted).value shouldBe "user-answer"

  "the crypto should be able to read pre-migration plain text values as fallback" in:
    val plainTextValue = PlainText("pre-migration-user-answer")
    val encryptedValue = NoCrypto.encrypt(plainTextValue)
    // here we use crypto.decrypt to decrypt a value that was encrypted with NoCrypto, which simulates a pre-migration value
    crypto.decrypt(encryptedValue).value shouldBe "pre-migration-user-answer"
