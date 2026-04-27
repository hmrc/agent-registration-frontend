/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.agentregistrationfrontend.modules

import com.google.inject.AbstractModule
import com.google.inject.Provides
import com.google.inject.name.Named
import com.typesafe.config.Config
import uk.gov.hmrc.crypto.Crypted
import uk.gov.hmrc.crypto.Decrypter
import uk.gov.hmrc.crypto.Encrypter
import uk.gov.hmrc.crypto.PlainBytes
import uk.gov.hmrc.crypto.PlainContent
import uk.gov.hmrc.crypto.PlainText
import uk.gov.hmrc.crypto.SymmetricCryptoFactory

import java.nio.charset.StandardCharsets
import java.util.Base64
import javax.inject.Singleton

class Module
extends AbstractModule:

  @Provides
  @Singleton
  @Named("fieldLevelEncryption")
  def crypto(config: Config): Encrypter & Decrypter =
    if (config.getBoolean("fieldLevelEncryption.enable"))
    then SymmetricCryptoFactory.aesCryptoFromConfig("fieldLevelEncryption", config)
    else NoCrypto

/** Encrypter/decrypter that does nothing (i.e. leaves content in plaintext). Only to be used for debugging.
  */
private trait NoCrypto
extends Encrypter,
  Decrypter:

  def encrypt(plain: PlainContent): Crypted =
    plain match
      case PlainText(text) => Crypted(text)
      case PlainBytes(bytes) => Crypted(new String(Base64.getEncoder.encode(bytes), StandardCharsets.UTF_8))
  def decrypt(notEncrypted: Crypted): PlainText = PlainText(notEncrypted.value)
  def decryptAsBytes(nullEncrypted: Crypted): PlainBytes = PlainBytes(Base64.getDecoder.decode(nullEncrypted.value))

object NoCrypto
extends NoCrypto
