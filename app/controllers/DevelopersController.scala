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

package controllers

import connectors.{AuthConnector, DeveloperConnector}
import model._
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.play.frontend.controller.FrontendController
import utils.{GatekeeperAuthProvider, GatekeeperAuthWrapper}
import views.html.developers.developers

object DevelopersController extends DevelopersController {
  override val developerConnector: DeveloperConnector = DeveloperConnector
  override def authConnector = AuthConnector
  override def authProvider = GatekeeperAuthProvider
}

trait DevelopersController extends FrontendController with GatekeeperAuthWrapper {

  val developerConnector: DeveloperConnector

  def developersPage: Action[AnyContent] = requiresRole(Role.APIGatekeeper) {
    implicit request => implicit hc =>
      for {
        devs: Seq[User] <- developerConnector.fetchAll
        emails: String = devs.map(dev => dev.email).mkString(",")
      } yield Ok(developers(devs, emails))
  }

}
