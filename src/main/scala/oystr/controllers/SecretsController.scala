package oystr.controllers

import akka.actor.ActorSystem
import javax.inject.{Inject, Singleton}
import oystr.akka.Inquire.inquire
import oystr.akka.RootActorsImpl
import oystr.domain._
import oystr.domain.json._
import oystr.services._
import oystr.utils.Utils._
import play.api.Configuration
import play.api.libs.json._
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal


@Singleton
class SecretsController @Inject() (
    services: BasicServices,
    actors: RootActorsImpl) extends InjectedController {
    implicit val ec: ExecutionContext = services.ec()
    implicit val sys: ActorSystem = services.sys()
    implicit val configuration: Configuration = services.conf()

    def createPolicyFor: Action[AnyContent] = Action.async { implicit request =>
        ¬¬ ({ m =>
            val metadata = Metadata(Some(m.account), None, None, None)
            ~> (CreatePolicyFor(metadata))

            Future.successful(Ok(Json.toJson(metadata)))
        }, requireVaultToken = false)
    }

    def createTokenFor: Action[JsValue] = Action.async(parse.json) { implicit request =>
        request.body.validate[TokenRequest] fold (
            errors => toJsError(errors),
            valid => {
                ¬¬({ m =>
                    val policyName = s"${m.account}-caging-policy"
                    val tokenRequest = valid
                        .copy(
                            policies = Some(Seq(policyName)),
                            meta = Some(TokenMetadata(m.account, m.username))
                        )
                    ~>(CreateTokenFor(tokenRequest))

                    Future.successful(Ok(Json.toJson(tokenRequest)))
                }, requireVaultToken = false)
            }
        )
    }

    private def toJsError(errors: Seq[(JsPath, Seq[JsonValidationError])]): Future[Result] =
        Future.successful(BadRequest(JsError.toJson(errors)))

    def storeCredentialsFor: Action[JsValue] = Action.async(parse.json) { implicit request =>
        request.body.validate[StoreDataRequest] fold (
            errors => toJsError(errors),
            storeDataRequest => {
                ¬¬ { m =>
                    ~> {
                        val bot = storeDataRequest.metadata.flatMap(_.bot)

                        val metadata = Metadata(Some(m.account), Some(m.username), bot, request.vaultToken())
                        val content = storeDataRequest.content

                        if (content.password.isDefined) {
                            StoreCredentialsFor(metadata, content)
                        } else {
                            StoreCertificateFor(metadata, content)
                        }
                    }
                }
            }
        )
    }

    def deleteDataFor(bot: String, username: String, target: String): Action[AnyContent] = Action.async {
        implicit request => ¬¬ { m => <~ (m.account, m.username, bot, username, target, delete = true) }
    }

    def dataFor(bot: String, username: String, target: String): Action[AnyContent] = Action.async {
        implicit request => ¬¬ { m => <~ (m.account, m.username, bot, username, target) }
    }

    // TODO: Rename this method to something more meaningful.
    def ¬¬(msg: MorbidUser => Future[Result], requireVaultToken: Boolean = true)
          (implicit request: Request[AnyRef]): Future[Result] = {
        if(request.validate(requireVaultToken = requireVaultToken)) {
            request
                .morbid()
                .map { _.head }
                .flatMap { msg }
                .recover { case NonFatal(e) => InternalServerError(e.getMessage) }
        } else {
            Future.successful(BadRequest(s"Missing required headers."))
        }
    }

    private def ~>(msg: Any)(implicit request: Request[AnyRef]): Future[Result] = {
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
    def <~(organization: Long, name: String, bot: String, username: String, target: String, delete: Boolean = false)
          (implicit request: Request[AnyContent]): Future[Result] = {
        ~> {
            val token = request.vaultToken()
            HandleDataFor(
                Metadata(Some(organization), Some(name), Some(bot), token),
                Content(username, None, None),
                target,
                delete=delete
            )
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
