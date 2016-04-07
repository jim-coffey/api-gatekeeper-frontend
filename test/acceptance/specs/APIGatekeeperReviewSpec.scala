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

import acceptance.pages.{ReviewPage, DashboardPage}
import acceptance.{SignInSugar, BaseSpec}
import com.github.tomakehurst.wiremock.client.WireMock._
import component.matchers.CustomMatchers
import org.scalatest.Matchers

class APIGatekeeperReviewSpec  extends BaseSpec with SignInSugar with Matchers with CustomMatchers with MockDataSugar {

  val approveRequest =
    s"""
       |{
       |  "gatekeeperUserId":"$gatekeeperId"
       |}
     """.stripMargin

  feature("Approve a request to uplift an application") {

    scenario("I see the review page and I am able to approve the uplift request") {

      stubFor(get(urlEqualTo("/gatekeeper/applications"))
        .willReturn(aResponse().withBody(applications).withStatus(200)))

      stubFor(get(urlEqualTo(s"/gatekeeper/application/$appPendingApprovalId1"))
        .willReturn(aResponse().withBody(application).withStatus(200)))

      val encodedEmail=URLEncoder.encode(adminEmail, "UTF-8")

      stubFor(get(urlEqualTo(s"/developer?email=$encodedEmail"))
        .willReturn(aResponse().withBody(administrator).withStatus(200)))

      stubFor(post(urlMatching(s"/application/$appPendingApprovalId1/approve-uplift"))
          .withRequestBody(equalToJson(approveRequest))
        .willReturn(aResponse().withStatus(200)))

      signInGatekeeper
      on(DashboardPage)
      clickOnLink(s"data-review-$appPendingApprovalId1")
      on(ReviewPage(appPendingApprovalId1, "First Application"))
      clickOnSubmit()
      on(DashboardPage)
    }

    scenario("I see the dashboard page when the request to uplift the application fails with a 412") {

      stubFor(get(urlEqualTo("/gatekeeper/applications"))
        .willReturn(aResponse().withBody(applications).withStatus(200)))

      stubFor(get(urlEqualTo(s"/gatekeeper/application/$appPendingApprovalId1"))
        .willReturn(aResponse().withBody(application).withStatus(200)))

      val encodedEmail=URLEncoder.encode(adminEmail, "UTF-8")

      stubFor(get(urlEqualTo(s"/developer?email=$encodedEmail"))
        .willReturn(aResponse().withBody(administrator).withStatus(200)))

      stubFor(post(urlMatching(s"/application/$appPendingApprovalId1/approve-uplift"))
        .withRequestBody(equalToJson(approveRequest))
        .willReturn(aResponse().withStatus(412)))

      signInGatekeeper
      on(DashboardPage)
      clickOnLink(s"data-review-$appPendingApprovalId1")
      on(ReviewPage(appPendingApprovalId1, "First Application"))
      clickOnSubmit()
      on(DashboardPage)
    }
  }
}
