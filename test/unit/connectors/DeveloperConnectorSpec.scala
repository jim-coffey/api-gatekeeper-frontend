package unit.connectors

import java.net.URLEncoder

import com.github.tomakehurst.wiremock.client.WireMock._
import config.WSHttp
import connectors.DeveloperConnector
import model.UserResponse
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterEach, Matchers}
import play.api.libs.json.Json
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet, HttpPost}
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import uk.gov.hmrc.time.DateTimeUtils


class DeveloperConnectorSpec extends UnitSpec with Matchers with ScalaFutures with WiremockSugar with BeforeAndAfterEach with WithFakeApplication {


  trait Setup {
    implicit val hc = HeaderCarrier()

    val connector = new DeveloperConnector {
      override val developerBaseUrl: String = wireMockUrl
      override val http: HttpPost with HttpGet = WSHttp
    }
  }


  "fetchByEmail" should {
    val developer1Email = "developer1@email.com"
    val developer2Email = "developer2@email.com"

    val timeStamp = DateTimeUtils.now

    def encode(str: String) = URLEncoder.encode(str, "UTF-8")

    def aUserResponse(email: String) = UserResponse(email, "first", "last", DateTimeUtils.now, DateTimeUtils.now)

    def verifyUserResponse(userResponse: UserResponse,
                           expectedEmail: String, expectedFirstName: String, expectedLastName: String) = {
      userResponse.email shouldBe expectedEmail
      userResponse.firstName shouldBe expectedFirstName
      userResponse.lastName shouldBe expectedLastName
    }


    "fetch developer by email" in new Setup {
      stubFor(get(urlEqualTo(s"/developer?email=${encode(developer1Email)}")).willReturn(
        aResponse().withStatus(200).withBody(
          Json.toJson(aUserResponse(developer1Email)).toString()))
      )
      val result = await(connector.fetchByEmail(developer1Email))
      verifyUserResponse(result,developer1Email,"first","last")
    }

    "fetch all developers by emails" in new Setup {
      stubFor(get(urlEqualTo(s"/developers?emails=$developer1Email,$developer2Email")).willReturn(
        aResponse().withStatus(200).withBody(
          Json.toJson(Seq(aUserResponse(developer1Email),aUserResponse(developer2Email))).toString()))
      )
      val result = await(connector.fetchByEmails(Seq(developer1Email,developer2Email)))
      verifyUserResponse(result(0),developer1Email,"first","last")
      verifyUserResponse(result(1),developer2Email,"first","last")
    }
  }
}
