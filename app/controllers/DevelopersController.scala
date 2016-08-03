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

import connectors.{ApiDefinitionConnector, ApplicationConnector, AuthConnector, DeveloperConnector}
import model._
import model.Forms._
import uk.gov.hmrc.play.frontend.controller.FrontendController
import utils.{GatekeeperAuthProvider, GatekeeperAuthWrapper}
import views.html.developers.developers

import scala.concurrent.Future

object DevelopersController extends DevelopersController {
  override val developerConnector: DeveloperConnector = DeveloperConnector
  override val apiDefinitionConnector = ApiDefinitionConnector
  override val applicationConnector = ApplicationConnector

  override def authConnector = AuthConnector

  override def authProvider = GatekeeperAuthProvider
}


trait DevelopersController extends FrontendController with GatekeeperAuthWrapper {

  val developerConnector: DeveloperConnector
  val apiDefinitionConnector: ApiDefinitionConnector
  val applicationConnector: ApplicationConnector

  private def filterByApi(devs: Seq[User], apps: Seq[ApplicationResponse])(filter: Option[String]): Seq[User] = {
    filter match {
      case None | Some("") => devs
      case Some(flt) => {
        val collaborators = apps.filter(app => app.subscriptions.exists(s => s.context == flt))
          .map(_.collaborators)
          .flatten
          .map(_.emailAddress)
          .toSet

        devs.filter(u => collaborators.contains(u.email))
      }
    }
  }

  private def redirect(filter: Option[String], pageNumber: Int, pageSize: Int) = {
    val pageParams = Map(
      "pageNumber" -> Seq(pageNumber.toString),
      "pageSize" -> Seq(pageSize.toString)
    )

    val queryParams = filter.fold(pageParams) { flt: String =>
      pageParams + ("filter" -> Seq(flt))
    }

    Redirect("", queryParams, 303)
  }

  def developersPage(filter: Option[String], pageNumber: Int, pageSize: Int) = requiresRole(Role.APIGatekeeper) {
    implicit request => implicit hc =>
      for {
        apps <- applicationConnector.fetchAllApplications
        devs <- developerConnector.fetchAll
        apis <- apiDefinitionConnector.fetchAll
        devsByApi = filterByApi(devs, apps)(filter)
        emails = devsByApi.map(dev => dev.email).mkString(",")
        page = PageableCollection(devsByApi, pageNumber, pageSize)
      } yield {
        if (page.valid) Ok(developers(page, emails, apis, filter))
        else redirect(filter, 1, pageSize)
      }
  }

  def submitDeveloperFilter = requiresRole(Role.APIGatekeeper) {
    implicit request => implicit hc =>
      val form = developerFilterForm.bindFromRequest.get
      Future.successful(redirect(Option(form.filter), form.pageNumber, form.pageSize))
    }
}
