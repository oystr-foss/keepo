package oystr.utils

import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.softwaremill.sttp._
import com.softwaremill.sttp.akkahttp.AkkaHttpBackend
import io.jsonwebtoken.Jwts
import org.apache.commons.lang3.StringUtils
import oystr.domain.json._
import oystr.domain.{Done, MorbidUser}
import play.api.Configuration
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Request

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps
import scala.util.control.NonFatal


object Utils {
    implicit class TokenHelper(request: Request[AnyRef]) {
        def vaultToken()(implicit configuration: Configuration): Option[String] = extract("vault")

        def morbidToken()(implicit configuration: Configuration): Option[String] = extract("morbid")

        def morbid()(implicit configuration: Configuration, ec: ExecutionContext): Future[Option[MorbidUser]] = {
            implicit val backend: SttpBackend[Future, Source[ByteString, Any]] = AkkaHttpBackend()
            val morbidHost = configuration.get[String]("morbid.host")
            val morbidPort = configuration.get[String]("morbid.port")
            val morbidAddr = s"http://$morbidHost:$morbidPort"

            def toJson(resp: (Int, JsValue)): Option[MorbidUser] = {
                Option {
                    if (resp._1 != 200) {
                        null
                    } else {
                        Json.fromJson[MorbidUser](resp._2).get
                    }
                }
            }

            def stripSignature(jwt: String): String = {
                val signatureIndex = jwt.lastIndexOf('.')
                val unsignedJwt = jwt.substring(0, signatureIndex + 1)
                String.valueOf(
                    Jwts
                        .parser()
                        .parseClaimsJwt(unsignedJwt)
                        .getBody
                        .get("sub")
                )
            }

            val maybeUser = for {
                token <- morbidToken().map(stripSignature)
                res   <- Option(
                         requestTo(sttp.get(uri"$morbidAddr/user/token/$token"), None, 120 seconds, toTuple)
                )
                fut   <- Option(res map { toJson })
            } yield fut recover {
                case NonFatal(e) => throw e
            }

            maybeUser get
        }

        def validate(requireVaultToken: Boolean)(implicit configuration: Configuration): Boolean = {
            morbidToken().isDefined && ((requireVaultToken && vaultToken().isDefined) || !requireVaultToken)
        }

        private def extract(key: String)(implicit configuration: Configuration): Option[String] = {
            val header = configuration.get[String](s"$key.header")
            request.headers.get(header).filter(StringUtils.isNotEmpty)
        }
    }

    type Reply = (Response[Array[Byte]], ByteString)

    def requestTo[T, S](req: com.softwaremill.sttp.Request[String, S], payload: Option[JsValue], timeout: Duration,
                        fn: Reply => T)(implicit backend: SttpBackend[Future, S], ec: ExecutionContext): Future[T] = {
        req
            .response(asByteArray)
            .readTimeout(timeout)
            .send()
            .map(resp =>
                resp.body match {
                    case Right(b) => fn(resp, ByteString(b))
                    case Left(err) => fn(resp, ByteString(err))
                }
            )
    }

    def toTuple(r: (Response[Array[Byte]], ByteString)): (Int, JsValue) = {
        val code = r._1.code
        val body = if (code.equals(204)) Json.toJson(Done())
                   else Json.parse(r._2.utf8String)

        (code, body)
    }
}