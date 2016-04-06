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

import java.net.URLEncoder

import acceptance.{SignInSugar, BaseSpec}
import acceptance.pages.{ReviewPage, DashboardPage, SignInPage}
import com.github.tomakehurst.wiremock.client.WireMock._
import component.matchers.CustomMatchers
import org.openqa.selenium.By
import org.scalatest.Matchers

class APIGatekeeperDashboardSpec extends BaseSpec with SignInSugar with Matchers with CustomMatchers with MockDataSugar{

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

    scenario("I see the message There are no pending applications when there are no applications awaiting uplift approval") {
      stubFor(get(urlEqualTo("/gatekeeper/applications"))
        .willReturn(aResponse().withBody("[]").withStatus(200)))

      signInGatekeeper
      on(DashboardPage)
      assertNoPendingApplications()
    }


    scenario("I can click on the Review button to be taken to the review page for an application awaiting uplift approval") {
      stubFor(get(urlEqualTo("/gatekeeper/applications"))
          .willReturn(aResponse().withBody(applications).withStatus(200)))

      stubFor(get(urlEqualTo(s"/gatekeeper/application/$appPendingApprovalId1"))
          .willReturn(aResponse().withBody(application).withStatus(200)))

      val encodedEmail=URLEncoder.encode(adminEmail, "UTF-8")

      stubFor(get(urlEqualTo(s"/developer?email=$encodedEmail"))
          .willReturn(aResponse().withBody(administrator).withStatus(200)))

      signInGatekeeper
      on(DashboardPage)
      clickOnLink(s"data-review-$appPendingApprovalId1")
      on(ReviewPage(appPendingApprovalId1, "First Application"))
      verifyText("data-description", applicationDescription)
      verifyText("data-submitter-name", fullName)
      verifyText("data-submitter-email", adminEmail)
    }
  }

  private def assertPendingApplication(appId: String, expected: String) = {
    webDriver.findElement(By.cssSelector(s"[data-pending-$appId]")).getText.replaceAll("\n", " ") shouldBe expected
  }

  private def assertNoPendingApplications() = {
    webDriver.findElement(By.cssSelector(s"[data-pending-none]")).getText shouldBe "There are no pending applications."
  }
}
