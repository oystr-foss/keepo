package oystr.controllers

import javax.inject.{Inject, Singleton}
import oystr.akka.Inquire.inquire
import oystr.akka.RootActorsImpl
import oystr.domain.json._
import oystr.domain.{Content, Metadata, StoreDataRequest, TokenRequest}
import oystr.services._
import play.api.libs.json.{JsSuccess, JsValue}
import play.api.mvc.{Action, AnyContent, InjectedController, Request, Result}

import scala.concurrent.Future

@Singleton
class SecretsController @Inject() (
    services: BasicServices,
    actors: RootActorsImpl) extends InjectedController {
    implicit val ec = services.ec()
    implicit val sys = services.sys()

    private val vaultHeader = services.conf().get[String]("vault.header")
    private val oystrHeader = services.conf().get[String]("oystr.header")

    def createPolicyFor: Action[JsValue] = Action.async(parse.json) {
        _.body.validate[Metadata] match {
            case success: JsSuccess[Metadata] => ~> { CreatePolicyFor(success.get) }
        }
    }

    def createTokenFor: Action[JsValue] = Action.async(parse.json) {
        _.body.validate[TokenRequest] match {
            case success: JsSuccess[TokenRequest] => ~> { CreateTokenFor(success.get) }
        }
    }

    def storeCredentialsFor: Action[JsValue] = Action.async(parse.json) { request =>
        request.body.validate[StoreDataRequest] match {
            case success: JsSuccess[StoreDataRequest] => ~> {
                if(request.headers.hasHeader(vaultHeader) || request.headers.hasHeader(oystrHeader)) {
                    val token = request.headers(vaultHeader)
                    val storeDataRequest = success.get
                    val metadata = storeDataRequest.metadata.copy(token = Some(token))
                    val content = storeDataRequest.content

                    if(content.password.isDefined) {
                        StoreCredentialsFor(metadata, content)
                    } else {
                        StoreCertificateFor(metadata, content)
                    }
                } else {
                    Future.successful(BadRequest(s"Missing $vaultHeader header."))
                }
            }
        }
    }

    def deleteDataFor(organization: String, name: String, bot: String, username: String, target: String): Action[AnyContent] = Action.async {
        implicit r => <~(organization, name, bot, username, target, delete = true)
    }

    def dataFor(organization: String, name: String, bot: String, username: String, target: String): Action[AnyContent] = Action.async {
        implicit r => <~(organization, name, bot, username, target)
    }

    private def ~>(msg: Any): Future[Result] = {
        inquire(actors.vault) {
            msg
        } flatMap {
            case future: Future[(Int, JsValue)] => future map toResponse
        } recover {
            case any => InternalServerError(s"Unknown internal server error. $any")
        }
    }

    /**
     * Handle data retrieval or deletion. Created like this to avoid code repetition.
     * @param delete - True to delete, else just retrieve information.
     */
    def <~(organization: String, name: String, bot: String, username: String, target: String, delete: Boolean = false)
          (implicit r: Request[AnyContent]): Future[Result] = {
        if(r.headers.hasHeader(vaultHeader)) {
            ~> {
                val token = r.headers(vaultHeader)

                HandleDataFor(
                    Metadata(organization, Some(name), Some(bot), Some(token)),
                    Content(username, None, None),
                    target,
                    delete=true
                )
            }
        } else {
            Future.successful(BadRequest(s"Missing $vaultHeader header."))
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
}
