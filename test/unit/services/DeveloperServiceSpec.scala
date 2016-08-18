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

import java.util.UUID

import connectors.{ApiDefinitionConnector, ApplicationConnector, DeveloperConnector}
import model._
import org.joda.time.DateTime
import org.mockito.BDDMockito._
import org.mockito.Matchers._
import org.mockito.Matchers.{eq => mEq}
import org.scalatest.mock.MockitoSugar
import services.DeveloperService
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future

class DeveloperServiceSpec extends UnitSpec with MockitoSugar {
  trait Setup {
    val testDeveloperService = new DeveloperService {
      val applicationConnector = mock[ApplicationConnector]
      val developerConnector = mock[DeveloperConnector]
      val apiDefinitionConnector = mock[ApiDefinitionConnector]
    }

    val users = Seq(
      User("sample@email.com", "Sample", "Email", false),
      User("another@email.com", "Sample2", "Email", true),
      User("someone@email.com", "Sample3", "Email", true))

    val collaborators = Set(
      Collaborator("sample@email.com", CollaboratorRole.ADMINISTRATOR),
      Collaborator("someone@email.com", CollaboratorRole.DEVELOPER))

    val allApplications = Seq(
      ApplicationResponse(UUID.randomUUID(),
        "application1", None, collaborators, DateTime.now(), ApplicationState(), Nil),
      ApplicationResponse(UUID.randomUUID(),
        "application2", None, collaborators, DateTime.now(), ApplicationState(), Nil),
      ApplicationResponse(UUID.randomUUID(),
        "application3", None, collaborators, DateTime.now(), ApplicationState(), Nil)
    )

    val filteredApplications = Seq(
      ApplicationResponse(UUID.randomUUID(),
        "application1", None, collaborators, DateTime.now(), ApplicationState(), Nil),
      ApplicationResponse(UUID.randomUUID(),
        "application3", None, collaborators, DateTime.now(), ApplicationState(), Nil)
    )

    given(testDeveloperService.applicationConnector.fetchAllApplications()(any[HeaderCarrier])).willReturn(
      Future.successful(allApplications))
    given(testDeveloperService.applicationConnector.fetchAllApplicationsBySubscription(
      mEq("subscription"))(any[HeaderCarrier])).willReturn(Future.successful(filteredApplications))
  }

  "developerService" should {
    "list all developers when filtering not provided" in new Setup {
      given(testDeveloperService.developerConnector.fetchAll()(any[HeaderCarrier])).willReturn(Future.successful(users))
      given(testDeveloperService.apiDefinitionConnector.fetchAll()(any[HeaderCarrier])).willReturn(Seq.empty[APIDefinition])

      val result: Seq[ApplicationResponse] = await(testDeveloperService.filteredApps(None)(new HeaderCarrier()))

      result shouldEqual allApplications
    }

    "list filtered developers when filtering is provided" in new Setup {
      given(testDeveloperService.developerConnector.fetchAll()(any[HeaderCarrier])).willReturn(Future.successful(users))
      given(testDeveloperService.apiDefinitionConnector.fetchAll()(any[HeaderCarrier])).willReturn(Seq.empty[APIDefinition])

      val result = await(testDeveloperService.filteredApps(Some("subscription"))(new HeaderCarrier()))

      result shouldBe filteredApplications
    }

    "Turn a list of users into an email list" in new Setup {
      val result = testDeveloperService.emailList(users)

      result shouldBe "sample@email.com,another@email.com,someone@email.com"
    }
  }
}

