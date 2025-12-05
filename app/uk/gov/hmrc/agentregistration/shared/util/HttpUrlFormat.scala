package uk.gov.hmrc.agentregistration.shared.util

import play.api.libs.json._

import java.net.URL
import scala.util.Try

object HttpUrlFormat:

  given format: Format[URL] =
    new Format[URL]:

      override def reads(json: JsValue): JsResult[URL] =
        json
          .validate[String]
          .flatMap(parseUrl(_).fold(invalidUrlError)(JsSuccess(_)))

      private def parseUrl(url: String): Option[URL] =
        Try(new java.net.URI(url).toURL).toOption

      private def invalidUrlError: JsError =
        JsError(Seq(JsPath() -> Seq(JsonValidationError("error.expected.url"))))

      override def writes(o: URL): JsValue =
        JsString(o.toString)
