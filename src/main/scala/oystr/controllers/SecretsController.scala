package oystr.controllers

import javax.inject.{Inject, Singleton}
import oystr.Utils._
import oystr.akka.Inquire.inquire
import oystr.akka.RootActorsImpl
import oystr.domain.json._
import oystr.domain.{Content, Metadata, StoreDataRequest, TokenRequest}
import oystr.services._
import play.api.libs.json.{JsSuccess, JsValue}
import play.api.mvc._

import scala.concurrent.Future

@Singleton
class SecretsController @Inject() (
    services: BasicServices,
    actors: RootActorsImpl) extends InjectedController {
    implicit val ec = services.ec()
    implicit val sys = services.sys()
    implicit val configuration = services.conf()

    private val vaultHeader = configuration.get[String]("vault.header")

    def createPolicyFor: Action[JsValue] = Action.async(parse.json) { implicit request =>
        request.body.validate[Metadata] match {
            case success: JsSuccess[Metadata] => ~> (CreatePolicyFor(success.get), requireVaultToken = false)
        }
    }

    def createTokenFor: Action[JsValue] = Action.async(parse.json) { implicit request =>
        request.body.validate[TokenRequest] match {
            case success: JsSuccess[TokenRequest] => ~> (CreateTokenFor(success.get), requireVaultToken = false)
        }
    }

    def storeCredentialsFor: Action[JsValue] = Action.async(parse.json) { implicit request =>
        request.body.validate[StoreDataRequest] match {
            case success: JsSuccess[StoreDataRequest] => ~> {
                val storeDataRequest = success.get
                val metadata = storeDataRequest.metadata.copy(token = request.vaultToken())
                val content = storeDataRequest.content

                if(content.password.isDefined) {
                    StoreCredentialsFor(metadata, content)
                } else {
                    StoreCertificateFor(metadata, content)
                }
            }
        }
    }

    def deleteDataFor(organization: String, name: String, bot: String, username: String, target: String):
        Action[AnyContent] = Action.async {
        implicit r => <~ (organization, name, bot, username, target, delete = true)
    }

    def dataFor(organization: String, name: String, bot: String, username: String, target: String):
        Action[AnyContent] = Action.async {
        implicit r => <~ (organization, name, bot, username, target)
    }

    private def ~>(msg: Any, requireVaultToken: Boolean = true)(implicit request: Request[AnyRef]): Future[Result] = {
        validateThen (
            inquire(actors.vault) {
                msg
            } flatMap {
                case future: Future[(Int, JsValue)] => future map toResponse
            } recover {
                case any => InternalServerError(s"Unknown internal server error. $any")
            },
            requireVaultToken
        )
    }

    /**
     * Handle data retrieval or deletion. Created like this to avoid code repetition.
     * @param delete - True to delete, else just retrieve information.
     */
    def <~(organization: String, name: String, bot: String, username: String, target: String, delete: Boolean = false)
          (implicit request: Request[AnyContent]): Future[Result] = {
        validateThen {
            ~> {
                val token = request.headers(vaultHeader)
                HandleDataFor(
                    Metadata(organization, Some(name), Some(bot), Some(token)),
                    Content(username, None, None),
                    target,
                    delete=true
                )
            }
        }
    }

    private def toResponse(res: (Int, JsValue)): Result = {
        res._1 match {
            case 200 => Ok(res._2)
            case 204 => NoContent
            case 400 => BadRequest(res._2)
            case 404 => NotFound(res._2)
            case 403 => Forbidden(res._2)
            case 503 => ServiceUnavailable(res._2)
            case 500 => InternalServerError(res._2)
            case any => InternalServerError(s"Couldn't handle request. $any")
        }
    }

    def validateThen(content: Future[Result], requireVaultToken: Boolean = true)
                    (implicit request: Request[AnyRef]): Future[Result] = {
        if(request.validate(requireVaultToken = requireVaultToken)) {
            content
        } else {
            Future.successful(BadRequest(s"Missing required headers."))
        }
    }
}
