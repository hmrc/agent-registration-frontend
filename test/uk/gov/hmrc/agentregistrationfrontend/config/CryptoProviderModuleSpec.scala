package uk.gov.hmrc.agentregistrationfrontend.config

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import uk.gov.hmrc.crypto.PlainText
import uk.gov.hmrc.crypto.SymmetricCryptoFactory

class CryptoProviderModuleSpec
extends AnyWordSpecLike
with Matchers:

  private val module = new CryptoProviderModule
  private val fieldLevelEncryptionKey = "edkOOwt7uvzw1TXnFIN6aRVHkfWcgiOrbBvkEQvO65g="

  "aesCryptoInstance" should:
    "encrypt new values with AES-GCM from field level encryption config" in:
      val crypto = module.crypto(configuration(fieldLevelEncryptionEnabled = true))
      val firstEncrypted = crypto.encrypt(PlainText("user-answer"))
      val secondEncrypted = crypto.encrypt(PlainText("user-answer"))
      firstEncrypted.value should not be "user-answer"
      secondEncrypted.value should not be "user-answer"
      firstEncrypted should not be secondEncrypted
      crypto.decrypt(firstEncrypted).value shouldBe "user-answer"
      crypto.decrypt(secondEncrypted).value shouldBe "user-answer"

    "decrypt old AES values using the same field level encryption key as fallback" in:
      val oldAesCrypto = SymmetricCryptoFactory.aesCrypto(fieldLevelEncryptionKey)
      val oldAesEncrypted = oldAesCrypto.encrypt(PlainText("pre-migration-answer"))
      val crypto = module.crypto(configuration(fieldLevelEncryptionEnabled = true))
      crypto.decrypt(oldAesEncrypted).value shouldBe "pre-migration-answer"

  private def configuration(fieldLevelEncryptionEnabled: Boolean): Config = ConfigFactory.parseString(
    s"""
       |fieldLevelEncryption {
       |  enable = $fieldLevelEncryptionEnabled
       |  key = "$fieldLevelEncryptionKey"
       |}
       |""".stripMargin
  )
