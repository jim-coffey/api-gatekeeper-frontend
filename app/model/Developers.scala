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

package model

case class Developers(users: Seq[User]) {

  private implicit val toDevelopers = (d: Seq[User]) => Developers(d)
  private val DELIMITER = "; "

  lazy val emailList = users.map(_.email).mkString(DELIMITER)

  def filterBy(filters: StatusFilter): Developers = {
    filters match {
      case AnyStatus => this
      case _ => users.filter(u => u.status == filters)
    }
  }
}