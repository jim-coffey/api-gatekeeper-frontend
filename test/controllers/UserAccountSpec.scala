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

import play.api.libs.json.Json
import play.api.test.{FakeRequest, Helpers}
import play.filters.csrf.CSRF.SignedTokenProvider
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

class UserAccountSpec extends UnitSpec with WithFakeApplication {

  Helpers.running(fakeApplication) {

    trait Setup {
      val underTest = new UserAccount {}
      val sessionParams = Seq("csrfToken" -> SignedTokenProvider.generateToken)
      val requestWhenLoggedOut = FakeRequest().withSession(sessionParams: _*)
    }

    "loginPage" should {
      "give 200" in new Setup {
        val result = await(underTest.loginPage()(requestWhenLoggedOut))
        status(result) shouldBe 200
      }
    }

    "authenticateAction" should {
      "give 200 when valid login form is posted" in new Setup {
        implicit val format = Json.format[LoginForm]
        val result = await(underTest.authenticateAction()(
          requestWhenLoggedOut.withJsonBody(Json.toJson(new LoginForm("username","password")))))
        status(result) shouldBe 200
      }

      "give 400 when an invalid login form is posted" in new Setup {
        implicit val format = Json.format[LoginForm]
        val result = await(underTest.authenticateAction()(
          requestWhenLoggedOut.withJsonBody(Json.toJson(new LoginForm("","password")))))
        status(result) shouldBe 400
      }
    }

  }
}
