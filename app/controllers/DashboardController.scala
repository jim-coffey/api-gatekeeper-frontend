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

import connectors.{DeveloperConnector, ApplicationConnector, AuthConnector}
import model.State.{State, _}
import model.UpliftAction.{UpliftAction, _}
import model._
import play.api.Logger
import play.api.data.Form
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.play.frontend.controller.FrontendController
import utils.{GatekeeperAuthProvider, GatekeeperAuthWrapper}
import views.html.approvedApplication._
import views.html.dashboard._
import views.html.review._

import scala.concurrent.Future

object DashboardController extends DashboardController {
  override def authProvider = GatekeeperAuthProvider

  override def authConnector = AuthConnector

  override val applicationConnector = ApplicationConnector
  override val developerConnector: DeveloperConnector = DeveloperConnector
}

trait DashboardController extends FrontendController with GatekeeperAuthWrapper {

  val applicationConnector: ApplicationConnector
  val developerConnector: DeveloperConnector

  def dashboardPage: Action[AnyContent] = requiresRole(Role.APIGatekeeper) {
    implicit request => implicit hc =>

      def applicationsForDashboard(apps: Seq[ApplicationWithUpliftRequest]): Map[String, Seq[ApplicationWithUpliftRequest]] = {
        val grouped: Map[State, Seq[ApplicationWithUpliftRequest]] = apps.groupBy(_.state)
        val pendingApproval = grouped.getOrElse(PENDING_GATEKEEPER_APPROVAL, Seq())
        val pendingVerification = grouped.getOrElse(PENDING_REQUESTER_VERIFICATION, Seq()) ++ grouped.getOrElse(PRODUCTION, Seq())

        Map("pendingApproval" -> pendingApproval.sortWith(ApplicationWithUpliftRequest.compareBySubmittedOn),
            "pendingVerification" -> pendingVerification.sortBy(_.name))
      }

      for {
        apps <- applicationConnector.fetchApplications()
        mappedApps = applicationsForDashboard(apps)
      } yield Ok(dashboard(mappedApps))
  }

  def reviewPage(appId: String): Action[AnyContent] = requiresRole(Role.APIGatekeeper) {
    implicit request => implicit hc =>

      def lastSubmission(app: ApplicationWithHistory): Future[SubmissionDetails] = {
        val submission: StateHistory = app.history.filter(_.state == State.PENDING_GATEKEEPER_APPROVAL)
          .sortWith(StateHistory.ascendingDateForAppId)
          .lastOption.getOrElse(throw new InconsistentDataState("pending gatekeeper approval state history item not found"))

        developerConnector.fetchByEmail(submission.actor.id).map(s =>
          SubmissionDetails(s"${s.firstName} ${s.lastName}", s.email, submission.changedAt)
        )
      }

      def applicationDetails(app: ApplicationResponse, submission: SubmissionDetails) = {
        ApplicationDetails(app.id.toString, app.name, app.description.getOrElse(""), submission)
      }

      for {
        app <- applicationConnector.fetchApplication(appId)
        submission <- lastSubmission(app)
        details = applicationDetails(app.application, submission)
      } yield Ok(review(details))
  }

  def approvedApplicationPage: Action[AnyContent] = requiresRole(Role.APIGatekeeper) {
    implicit request => implicit hc =>
      Future.successful(Ok(approved()))
  }

  def handleUplift(appId: String): Action[AnyContent] = requiresRole(Role.APIGatekeeper) {
    implicit request => implicit hc =>
      val requestForm = HandleUpliftForm.form.bindFromRequest

      def errors(errors: Form[HandleUpliftForm]) =
        Future.successful(Redirect(controllers.routes.DashboardController.dashboardPage))

      def addApplicationWithValidForm(validForm: HandleUpliftForm) = {
        UpliftAction.from(validForm.action) match {
          case Some(APPROVE) =>
            applicationConnector.approveUplift(appId, loggedIn.get) map (
              ApproveUpliftSuccessful => Redirect(controllers.routes.DashboardController.dashboardPage)) recover {
              case e:ApproveUpliftPreconditionFailed => {
                Logger.warn("Request to uplift application failed as application might have already been uplifted.", e)
                Redirect(controllers.routes.DashboardController.dashboardPage)
              }
            }
          case Some(REJECT) =>
            Future.successful(Redirect(controllers.routes.DashboardController.dashboardPage))
        }
      }

      requestForm.fold(errors, addApplicationWithValidForm)
  }
}
