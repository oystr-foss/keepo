package keepo.services

import akka.actor.{Actor, ActorLogging}
import javax.inject.Inject
import keepo.domain.json._
import keepo.domain._
import keepo.utils.Utils._
import keepo.vaultclient.SttpVaultClient
import play.api.libs.json.{JsValue, Json}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps
import scala.util.control.NonFatal


case class HandleDataFor(metadata: Metadata, content: Content, target: String, delete: Boolean)
case class CreatePolicyFor(metadata: Metadata)
case class CreateTokenFor(tokenRequest: TokenRequest)
case class StoreCredentialsFor(metadata: Metadata, content: Content)
case class StoreCertificateFor(metadata: Metadata, content: Content)

sealed trait Vault {
    def createPolicy(metadata: Metadata): Future[(Int, JsValue)]
    def createToken(tokenRequest: TokenRequest): Future[(Int, JsValue)]
    def storeCredentials(metadata: Metadata, content: Content): Future[(Int, JsValue)]
    def storeCertificate(metadata: Metadata, content: Content): Future[(Int, JsValue)]
    def dataFor(metadata: Metadata, content: Content, target: String = "credentials"): Future[(Int, JsValue)]
}

class VaultActor @Inject()(
    services: BasicServices,
    client: SttpVaultClient) extends Vault with Actor with ActorLogging {
    implicit val ec: ExecutionContext = services.ec()

    private val rootToken = services.conf().get[String]("vault.root-token")

    def deleteDataFor(metadata: Metadata, content: Content, target: String = "credentials"): Future[(Int, JsValue)] = {
        process(metadata, {
            org =>
                val (token: String, bot: String, name: String, username: String) = getDataFrom(metadata, content)

                client.delete(s"/v1/secret/data/$org/$name/$bot/$username/$target",
                    token, 120 seconds) {
                    toTuple
                } recover {
                    case NonFatal(ex) => toError(ex)
                }
        })
    }

    override def createPolicy(metadata: Metadata): Future[(Int, JsValue)] = {
        process(metadata, {
            org =>
                val payload = s"""path \"secret/data/$org/*\" {\n\tcapabilities = [\"create\", \"read\", \"update\", \"delete\", \"list\"]\n}""".stripMargin
                val asJson = Json.toJson(Policy(policy = payload))

                post(s"/v1/sys/policy/$org-caging-policy", asJson, rootToken)
        })
    }

    override def createToken(tokenRequest: TokenRequest): Future[(Int, JsValue)] = {
        val asJson = Json.toJson(tokenRequest)

        post(s"/v1/auth/token/create", asJson, rootToken)
    }

    override def storeCredentials(metadata: Metadata, content: Content): Future[(Int, JsValue)] = {
        process(metadata, {
            org =>
                val (token: String, bot: String, name: String, username: String) = getDataFrom(metadata, content)
                val password = content.password getOrElse ""
                val credentialsRequest = CredentialsRequest(data = Credentials(username = username, password = password))
                val asJson = Json.toJson(credentialsRequest)

                post(s"/v1/secret/data/$org/$name/$bot/$username/credentials", asJson, token)
        })
    }

    override def storeCertificate(metadata: Metadata, content: Content): Future[(Int, JsValue)] = {
        process(metadata, {
            org =>
                val (token: String, bot: String, name: String, username: String) = getDataFrom(metadata, content)
                val certificate = Certificate(
                    username = username,
                    certificate = content.certificate getOrElse ""
                )
                val credentialsRequest = CredentialsRequest(data = certificate)
                val asJson = Json.toJson(credentialsRequest)

                post(s"/v1/secret/data/$org/$name/$bot/$username/certificate", asJson, token)
        })
    }

    override def dataFor(metadata: Metadata, content: Content, target: String = "credentials"): Future[(Int, JsValue)] = {
        process(metadata, {
            org =>
                val (token: String, bot: String, name: String, username: String) = getDataFrom(metadata, content)

                client.get(s"/v1/secret/data/$org/$name/$bot/$username/$target",
                    token, 120 seconds) {
                    toTuple
                } recover {
                    case NonFatal(ex) => toError(ex)
                }
        })
    }

    override def receive: Receive = {
        case HandleDataFor(metadata, content, target, false) => sender ! dataFor(metadata, content, target)
        case CreatePolicyFor(metadata) => sender ! createPolicy(metadata)
        case CreateTokenFor(tokenRequest) => sender ! createToken(tokenRequest)
        case StoreCredentialsFor(metadata, content) => sender ! storeCredentials(metadata, content)
        case StoreCertificateFor(metadata, content) => sender ! storeCertificate(metadata, content)
        case HandleDataFor(metadata, content, target, true) => sender ! deleteDataFor(metadata, content, target)
        case any => println(s"Unknown message $any")
    }

    private def post(url: String, payload: JsValue, token: String): Future[(Int, JsValue)] =
        client.post(url, Some(payload), token, 120 seconds) {
            toTuple
        } recover {
            case NonFatal(ex) => toError(ex)
        }

    private def toError(ex: Throwable): (Int, JsValue) = {
        log.debug(ex.getMessage)
        (500, Json.toJson(Error(ex.getMessage)))
    }

    private def getDataFrom(metadata: Metadata, content: Content): (String, String, String, String) = {
        val token = metadata.token getOrElse ""
        val bot = metadata.bot getOrElse ""
        val name = metadata.name getOrElse ""
        val username = content.username
        (token, bot, name, username)
    }

    def process(metadata: Metadata, msg: Long => Future[(Int, JsValue)]): Future[(Int, JsValue)] = {
        metadata.organization.map(msg).head
    }
}