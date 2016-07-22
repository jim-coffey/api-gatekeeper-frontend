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

package unit.controllers

import connectors.AuthConnector.InvalidCredentials
import connectors.{ApplicationConnector, AuthConnector, DeveloperConnector}
import controllers.DevelopersController
import model.LoginDetails.{JsonStringDecryption, JsonStringEncryption}
import model._
import org.joda.time.DateTime
import org.mockito.ArgumentCaptor
import org.mockito.BDDMockito._
import org.mockito.Matchers._
import org.scalatest.mock.MockitoSugar
import play.api.libs.json.Json
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import play.filters.csrf.CSRF.SignedTokenProvider
import uk.gov.hmrc.crypto.Protected
import uk.gov.hmrc.play.frontend.auth.AuthenticationProvider
import uk.gov.hmrc.play.http.{HeaderCarrier, SessionKeys}
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

import scala.concurrent.Future

class DeveloperControllerSpec extends UnitSpec with MockitoSugar with WithFakeApplication {

  Helpers.running(fakeApplication) {

    trait Setup {
      val underTest = new DevelopersController {
        val authConnector = mock[AuthConnector]
        val authProvider = mock[AuthenticationProvider]
        val applicationConnector = mock[ApplicationConnector]
        val developerConnector = mock[DeveloperConnector]
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

    "developersPage" should {

      "go to loginpage with error if user is not authenticated" in new Setup {
        val loginDetails = LoginDetails("userName", Protected("password"))
        given(underTest.authConnector.login(any[LoginDetails])(any[HeaderCarrier])).willReturn(Future.failed(new InvalidCredentials))

        val result = await(underTest.developersPage()(aLoggedOutRequest))

        redirectLocation(result) shouldBe Some("/api-gatekeeper/login")
      }


      "load successfully if user is authenticated and authorised" in new Setup {
        val loginDetails = LoginDetails("userName", Protected("password"))
        val successfulAuthentication = SuccessfulAuthentication(BearerToken("bearer-token", DateTime.now().plusMinutes(10)), "userName", None)

        given(underTest.authConnector.login(any[LoginDetails])(any[HeaderCarrier])).willReturn(Future.successful(successfulAuthentication))
        given(underTest.authConnector.authorized(any[Role])(any[HeaderCarrier])).willReturn(Future.successful(true))
        given(underTest.developerConnector.fetchAll()(any[HeaderCarrier])).willReturn(Future.successful(Seq.empty[User]))

        val result = await(underTest.developersPage()(aLoggedInRequest))

        status(result) shouldBe 200
        bodyOf(result) should include("Dashboard")
      }

      "go to unauthorised page if user is not authorised" in new Setup {
        val loginDetails = LoginDetails("userName", Protected("password"))
        val successfulAuthentication = SuccessfulAuthentication(BearerToken("bearer-token", DateTime.now().plusMinutes(10)), "userName", None)

        given(underTest.authConnector.login(any[LoginDetails])(any[HeaderCarrier])).willReturn(Future.successful(successfulAuthentication))
        given(underTest.authConnector.authorized(any[Role])(any[HeaderCarrier])).willReturn(Future.successful(false))

        val result = await(underTest.developersPage()(aLoggedInRequest))

        status(result) shouldBe 401
        bodyOf(result) should include("Only Authorised users can access the requested page")
      }

    }

    "developersPage" should {
      val applicationId = "applicationId"
      val userName = "userName"

      "call developer api and list all developers" in new Setup {
        val loginDetails = LoginDetails("userName", Protected("password"))
        val successfulAuthentication = SuccessfulAuthentication(BearerToken("bearer-token", DateTime.now().plusMinutes(10)), userName, None)

        val users = Seq(User("sample@email.com", "Sample", "Email"), User("another@email.com", "Sample2", "Email"))

        given(underTest.authConnector.login(any[LoginDetails])(any[HeaderCarrier])).willReturn(Future.successful(successfulAuthentication))
        given(underTest.authConnector.authorized(any[Role])(any[HeaderCarrier])).willReturn(Future.successful(true))
        given(underTest.developerConnector.fetchAll()(any[HeaderCarrier])).willReturn(Future.successful(users))

        val appIdCaptor = ArgumentCaptor.forClass(classOf[String])
        val gatekeeperIdCaptor = ArgumentCaptor.forClass(classOf[String])

        val result = await(underTest.developersPage()(aLoggedInRequest))

        status(result) shouldBe 200
        users.foreach(user => bodyOf(result) should include(user.email))
      }
    }

  }
}
