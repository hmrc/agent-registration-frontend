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

package uk.gov.hmrc.agentregistrationfrontend.ispecs.wiremock

object FreePortFinder {

  //TODO: discuss if this is really needed, all tests are run in containers, default port should be always free
  def findFreePort(initial: Int = 11111): Int = {
    import java.net.ServerSocket

    def isPortFree(port: Int): Boolean = {
      try {
        val socket = new ServerSocket(port)
        socket.close()
        true
      } catch {
        case _: Exception => false
      }
    }

    Iterator.from(initial).find(isPortFree).getOrElse(
      throw new RuntimeException("No free port found")
    )
  }

}
