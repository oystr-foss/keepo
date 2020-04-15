package keepo.vaultclient

import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.softwaremill.sttp._
import com.softwaremill.sttp.akkahttp.AkkaHttpBackend
import javax.inject.{Inject, Singleton}
import keepo.services.BasicServices
import keepo.utils.Utils._
import play.api.libs.json.JsValue

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps


trait VaultClient {
    type Reply = (Response[Array[Byte]], ByteString)

    def get[T](url: String, token: String, timeout: Duration = 120 seconds)(fn: Reply => T): Future[T]
    def post[T](url: String, payload: Option[JsValue], token: String, timeout: Duration = 120 seconds)
               (fn: Reply => T): Future[T]
    def delete[T](url: String, token: String, timeout: Duration = 120 seconds)(fn: Reply => T): Future[T]
}

@Singleton
class SttpVaultClient @Inject() (
    services: BasicServices) extends VaultClient {
    implicit val ec: ExecutionContext = services.ec()
    implicit val backend: SttpBackend[Future, Source[ByteString, Any]] = AkkaHttpBackend()

    private val vaultHost = services.conf().get[String]("vault.host")
    private val vaultPort = services.conf().get[String]("vault.port")
    private val vaultAddr = s"http://$vaultHost:$vaultPort"

    private val vaultHeader = services.conf().get[String]("vault.header")

    override def get[T](url: String, token: String, timeout: Duration = 120 seconds)(fn: Reply => T): Future[T] = {
        val req = sttp
            .get(uri"${vaultAddr + url}")
            .header(vaultHeader, token)
        requestTo(
            req,
            Option.empty,
            timeout,
            fn
        )
    }

    override def post[T](url: String, payload: Option[JsValue], token: String, timeout: Duration)
                        (fn: Reply => T): Future[T] = {
        val body = payload map { _.toString } orNull

        val req = sttp
            .post(uri"${vaultAddr + url}")
            .header(vaultHeader, token)
            .body(body)
            .contentType(if (payload == null) "text/plain" else "application/json")
        requestTo(
            req,
            payload,
            timeout,
            fn
        )
    }

    override def delete[T](url: String, token: String, timeout: Duration = 120 seconds)(fn: Reply => T): Future[T] = {
        val req = sttp
            .delete(uri"${vaultAddr + url}")
            .header(vaultHeader, token)
        requestTo(
            req,
            Option.empty,
            timeout,
            fn
        )
    }
}
