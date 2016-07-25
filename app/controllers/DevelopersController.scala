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

import connectors.{ApiDefinitionConnector, AuthConnector, DeveloperConnector}
import model._
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.play.frontend.controller.FrontendController
import utils.{GatekeeperAuthProvider, GatekeeperAuthWrapper}
import views.html.developers.developers

object DevelopersController extends DevelopersController {
  override val developerConnector: DeveloperConnector = DeveloperConnector
  override def authConnector = AuthConnector
  override def authProvider = GatekeeperAuthProvider
  override val apiDefinitionConnector = ApiDefinitionConnector
}

trait DevelopersController extends FrontendController with GatekeeperAuthWrapper {

  val developerConnector: DeveloperConnector
  val apiDefinitionConnector: ApiDefinitionConnector

  case class DeveloperFilter(api : String)

  object DeveloperFilter {
    implicit val jsonFormat = Json.format[DeveloperFilter]
  }

  val developerFilterForm: Form[DeveloperFilter] = Form(
    mapping(
      "api" -> nonEmptyText(maxLength = 70)
    )(DeveloperFilter.apply)(DeveloperFilter.unapply))

  def developersPage: Action[AnyContent] = requiresRole(Role.APIGatekeeper) {
    implicit request => implicit hc =>
      for {
        devs: Seq[User] <- developerConnector.fetchAll
        apis <- apiDefinitionConnector.fetchAll
        emails: String = devs.map(dev => dev.email).mkString(",")
      } yield Ok(developers(devs, emails, apis))
  }

  def submitDeveloperFilter = Action { implicit request =>
    val filter: DeveloperFilter = developerFilterForm.bindFromRequest.get
    val api = filter.api
    Ok(s"SOMETHING HERE TO RE-RUN THE FETCH REQUEST $api")
  }
}
