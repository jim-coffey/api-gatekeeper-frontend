/*
 * Copyright 2016 HM Revenue & Customs
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

package connectors

import model.{APIDefinition, DefinitionFormats}
import play.api.Play._
import play.api.libs.ws.WS
import uk.gov.hmrc.play.config.ServicesConfig

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait ApiDefinitionConnector {
  val serviceUrl: String

  def fetchAll(): Future[Seq[APIDefinition]] = {
    val url = s"$serviceUrl/api-definition"

    WS.url(url).withHeaders("Content-Type" -> "application/json").get().map { result =>
      result.status match {
        case 200 => result.json.as[Seq[APIDefinition]]
        case _ => throw new RuntimeException(s"Unexpected response from $url: (${result.status}) ${result.body}")
      }
    }
  }
}

object ApiDefinitionConnector extends ApiDefinitionConnector with ServicesConfig {
  lazy val serviceUrl = baseUrl("api-definition")
}
