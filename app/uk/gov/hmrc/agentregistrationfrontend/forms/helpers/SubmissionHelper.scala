package uk.gov.hmrc.agentregistrationfrontend.forms.helpers

import play.api.mvc.AnyContent
import uk.gov.hmrc.agentregistrationfrontend.action.AgentApplicationRequest
import uk.gov.hmrc.agentregistrationfrontend.model.SubmitAction

object SubmissionHelper {
  def getSubmitAction(request: AgentApplicationRequest[AnyContent]): SubmitAction =
    SubmitAction.fromSubmissionWithDefault(
      request.body.asFormUrlEncoded
        .flatMap(_.get("submit")
          .flatMap(_.headOption))
    )
}
