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

import connectors.AuthConnector
import connectors.AuthConnector.InvalidCredentials
import model.LoginDetails.{JsonStringDecryption, JsonStringEncryption}
import model._
import org.joda.time.DateTime
import org.mockito.BDDMockito._
import org.mockito.Matchers._
import org.scalatest.mock.MockitoSugar
import play.api.libs.json.Json
import play.api.test.{FakeRequest, Helpers}
import play.filters.csrf.CSRF.SignedTokenProvider
import uk.gov.hmrc.crypto.Protected
import uk.gov.hmrc.play.frontend.auth.AuthenticationProvider
import uk.gov.hmrc.play.http.{HeaderCarrier, SessionKeys}
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

import scala.concurrent.Future

class DashboardControllerSpec extends UnitSpec with MockitoSugar with WithFakeApplication {

  Helpers.running(fakeApplication) {

    trait Setup {
      val underTest = new DashboardController {
        val authConnector = mock[AuthConnector]
        val authProvider = mock[AuthenticationProvider]
      }

      implicit val encryptedStringFormats = JsonStringEncryption
      implicit val decryptedStringFormats = JsonStringDecryption
      implicit val format = Json.format[LoginDetails]

      val csrfToken = "csrfToken" -> SignedTokenProvider.generateToken
      val authToken = SessionKeys.authToken -> "some-bearer-token"
      val userToken = GatekeeperSessionKeys.LoggedInUser -> "userName"

      val aLoggedInRequest = FakeRequest().withSession(csrfToken, authToken, userToken)
      val aLoggedOutRequest = FakeRequest().withSession(csrfToken)

    }


    "dashboardPage" should {

      "go to loginpage with error if user is not authenticated" in new Setup {
        val loginDetails = LoginDetails("userName", Protected("password"))
        given(underTest.authConnector.login(any[LoginDetails])(any[HeaderCarrier])).willReturn(Future.failed(new InvalidCredentials))

        val result = await(underTest.dashboardPage()(aLoggedOutRequest))

        status(result) shouldBe 303
        result.header.headers should contain("Location" -> "/api-gatekeeper/login")
      }


      "load successfully if user is authenticated and authorised" in new Setup {
        val loginDetails = LoginDetails("userName", Protected("password"))
        val successfulAuthentication = SuccessfulAuthentication(BearerToken("bearer-token", DateTime.now().plusMinutes(10)), "userName", None)

        given(underTest.authConnector.login(any[LoginDetails])(any[HeaderCarrier])).willReturn(Future.successful(successfulAuthentication))
        given(underTest.authConnector.authorized(any[Role])(any[HeaderCarrier])).willReturn(Future.successful(true))

        val result = await(underTest.dashboardPage()(aLoggedInRequest))

        status(result) shouldBe 200
        bodyOf(result) should include("Dashboard")
      }

      "go to unauthorised page if user is not authorised" in new Setup {
        val loginDetails = LoginDetails("userName", Protected("password"))
        val successfulAuthentication = SuccessfulAuthentication(BearerToken("bearer-token", DateTime.now().plusMinutes(10)), "userName", None)

        given(underTest.authConnector.login(any[LoginDetails])(any[HeaderCarrier])).willReturn(Future.successful(successfulAuthentication))
        given(underTest.authConnector.authorized(any[Role])(any[HeaderCarrier])).willReturn(Future.successful(false))

        val result = await(underTest.dashboardPage()(aLoggedInRequest))

        status(result) shouldBe 401
        bodyOf(result) should include("Only Authorised user can access the requested page")

      }

    }

  }
}
