package uk.gov.hmrc.agentregistration.shared.upscan

import java.util.UUID
import play.api.mvc.QueryStringBindable

final case class UploadId(value: String) extends AnyVal

object UploadId:
  def generate(): UploadId =
    UploadId(UUID.randomUUID().toString)

  implicit def queryBinder(implicit stringBinder: QueryStringBindable[String]): QueryStringBindable[UploadId] =
    stringBinder.transform(UploadId(_), _.value)