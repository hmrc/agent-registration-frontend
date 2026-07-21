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

package uk.gov.hmrc.agentregistrationfrontend.testonly.model

/** Convenience links to Kibana log searches for MDTP microservices, used from test-only pages. These are fixed per environment/service rather than sourced from
  * config, since they're developer navigation aids rather than application behaviour.
  */
object KibanaLinks:

  val riskingLogsQa: String =
    "https://kibana.tools.qa.tax.service.gov.uk/app/discover#/?_g=(filters:!(),refreshInterval:(pause:!t,value:60000),time:(from:now-1d,to:now))&_a=(columns:!(message,logger,level),dataSource:(dataViewId:match_all_logstash_ingested_logs_kibana_index_pattern,type:dataView),filters:!(),interval:auto,query:(language:lucene,query:'app.raw:%22agent-registration-risking%22'),sort:!(!('@timestamp',desc)))"

  val riskingLogsStaging: String =
    "https://kibana.tools.staging.tax.service.gov.uk/app/discover#/?_g=(filters:!(),refreshInterval:(pause:!t,value:60000),time:(from:now-15d,to:now))&_a=(columns:!(message,logger,level),dataSource:(dataViewId:match_all_logstash_ingested_logs_kibana_index_pattern,type:dataView),filters:!(),interval:auto,query:(language:lucene,query:'app.raw:%22agent-registration-risking%22'),sort:!(!('@timestamp',desc)))"
