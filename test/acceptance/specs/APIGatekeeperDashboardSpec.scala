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

package acceptance.specs

import acceptance.{SignInSugar, BaseSpec}
import acceptance.pages.{DashboardPage, SignInPage}
import com.github.tomakehurst.wiremock.client.WireMock._
import component.matchers.CustomMatchers
import org.openqa.selenium.By
import org.scalatest.Matchers

class APIGatekeeperDashboardSpec extends BaseSpec with SignInSugar with Matchers with CustomMatchers {

  val appPendingApprovalId1 = "df0c32b6-bbb7-46eb-ba50-e6e5459162ff"
  val appPendingApprovalId2 = "a4b47c82-5888-41fd-aa83-da2bbd4679d1"

  val applications =
    s"""
      |[
      |  {
      |    "id": "${appPendingApprovalId2}",
      |    "name": "Second Application",
      |    "submittedOn": 1458832690624,
      |    "state": "PENDING_GATEKEEPER_APPROVAL"
      |  },
      |  {
      |    "id": "${appPendingApprovalId1}",
      |    "name": "First Application",
      |    "submittedOn": 1458659208000,
      |    "state": "PENDING_GATEKEEPER_APPROVAL"
      |  },
      |  {
      |    "id": "9688ad02-230e-42b7-8f9a-be593565bfdc",
      |    "name": "Third",
      |    "submittedOn": 1458831410657,
      |    "state": "PENDING_REQUESTER_VERIFICATION"
      |  },
      |  {
      |    "id": "56148b28-65b0-47dd-a3ce-2f02840ddd31",
      |    "name": "Fourth",
      |    "submittedOn": 1458832728156,
      |    "state": "PRODUCTION"
      |  }
      |]
    """.stripMargin

  feature("View applications pending gatekeeper approval on the dashboard") {

    info("In order to manage uplift application requests")
    info("As a gatekeeper")
    info("I see a list of applications pending approval")

    scenario("I see a list of pending applications in ascending order by submitted date") {

      stubFor(get(urlEqualTo("/gatekeeper/applications"))
        .willReturn(aResponse().withBody(applications).withStatus(200)))

      signInGatekeeper
      on(DashboardPage)

      DashboardPage.bodyText should containInOrder(List("First Application", "Second Application"))
      assertPendingApplication(appPendingApprovalId1, "First Application submitted: 22.03.2016 Review")
      assertPendingApplication(appPendingApprovalId2, "Second Application submitted: 24.03.2016 Review")
    }

    scenario("There are no pending applications.") {
      stubFor(get(urlEqualTo("/gatekeeper/applications"))
        .willReturn(aResponse().withBody("[]").withStatus(200)))

      signInGatekeeper
      on(DashboardPage)
      assertNoPendingApplications()
    }
  }

  private def assertPendingApplication(appId: String, expected: String) = {
    webDriver.findElement(By.cssSelector(s"[data-pending-$appId]")).getText.replaceAll("\n", " ") shouldBe expected
  }

  private def assertNoPendingApplications() = {
    webDriver.findElement(By.cssSelector(s"[data-pending-none]")).getText shouldBe "There are no pending applications."
  }
}
