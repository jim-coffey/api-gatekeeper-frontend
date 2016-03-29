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

import acceptance.BaseSpec
import acceptance.pages.{DashboardPage, SignInPage}
import com.github.tomakehurst.wiremock.client.WireMock._

class APIGatekeeperDashboardSpec extends BaseSpec {

  val applications =
    """
      |[
      |  {
      |    "id": "df0c32b6-bbb7-46eb-ba50-e6e5459162ff",
      |    "name": "First",
      |    "submittedOn": 1458832690624,
      |    "state": "PENDING_GATEKEEPER_APPROVAL"
      |  },
      |  {
      |    "id": "a4b47c82-5888-41fd-aa83-da2bbd4679d1",
      |    "name": "Second",
      |    "submittedOn": 1458818916151,
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

  feature("View the applications on the dashboard") {

    info("In order to manage uplift application requests")
    info("As a gatekeeper")
    info("I see a list of applications")

    scenario("I see a list of pending application in ascending order by submitted date") {

      stubFor(get(urlEqualTo("/gatekeeper/applications"))
        .willReturn(aResponse().withBody(applications).withStatus(200)))

      signIn
      on(DashboardPage)
    }

    scenario("There are no pending applications.") (pending)
  }

  def signIn = {
    val authBody =
      """
        |{
        | "access_token": {
        |     "authToken":"Bearer fggjmiJzyVZrR6/e39TimjqHyla3x8kmlTd",
        |     "expiry":1459365831061
        |     },
        |     "expires_in":14400,
        |     "roles":[{"scope":"api","name":"gatekeeper"}],
        |     "authority_uri":"/auth/oid/joe.blogs",
        |     "token_type":"Bearer"
        |}
      """.stripMargin

    stubFor(post(urlEqualTo("/auth/authenticate/user"))
      .willReturn(aResponse().withBody(authBody).withStatus(200)))

    stubFor(get(urlEqualTo("/auth/authenticate/user/authorise?scope=api&role=gatekeeper"))
      .willReturn(aResponse().withStatus(200)))

    goOn(SignInPage)

    SignInPage.signIn("joe.blogs", "password")
  }
}
