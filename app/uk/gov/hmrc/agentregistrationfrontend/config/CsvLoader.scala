/*
 * Copyright 2023 HM Revenue & Customs
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

import scala.collection.immutable.ListMap
import scala.io.Source
import scala.util.Failure
import scala.util.Success
import scala.util.Try

object CsvLoader {

  def load(
    resourceName: String,
    namesAsValues: Boolean = false
  ): ListMap[String, String] =
    Try {
      require(resourceName.nonEmpty, "The file path should not be empty")
      require(resourceName.endsWith(".csv"), "The file should be a csv file")

      Source
        .fromInputStream(getClass.getResourceAsStream(resourceName), "utf-8")
        .getLines
        .drop(1)
        .foldLeft(ListMap.empty[String, String]) { (acc, row) =>
          val Array(code, name) = row.split(",", 2)
          if namesAsValues then acc.+(name.trim -> name.trim)
          else acc.+(code.trim -> name.trim)
        }
    } match {
      case Success(kvMap) if kvMap.nonEmpty => kvMap
      case Failure(ex) => sys.error(ex.getMessage)
      case _ => sys.error("No keys or values found")
    }

}
