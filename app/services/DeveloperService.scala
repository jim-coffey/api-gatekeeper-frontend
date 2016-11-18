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

package services

import connectors.DeveloperConnector
import model._
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object DeveloperService extends DeveloperService {
  override val developerConnector: DeveloperConnector = DeveloperConnector
}

trait DeveloperService {

  val developerConnector: DeveloperConnector

  def filterUsersBy(filter: ApiFilter[String], apps: Seq[ApplicationResponse])(users: Seq[User]): Seq[User] = {
    val collaborators = apps.flatMap(_.collaborators).map(_.emailAddress).toSet
    val unregistered = collaborators.diff(users.map(_.email).toSet).map(UnregisteredCollaborator(_))

    filter match {
      case AllUsers => users ++ unregistered
      case NoApplications => users.filterNot(u => collaborators.contains(u.email))
      case NoSubscriptions | OneOrMoreSubscriptions | OneOrMoreApplications | Value(_) => users.filter(u => collaborators.contains(u.email)) ++ unregistered
    }
  }

  def filterUsersBy(filter: StatusFilter)(users: Seq[User]): Seq[User] = {
    filter match {
      case AnyStatus => users
      case _ => users.filter(u => u.status == filter)
    }
  }

  def fetchDevelopers(implicit hc: HeaderCarrier): Future[Seq[User]] = {
    developerConnector.fetchAll.map(_.sorted)
  }
}
