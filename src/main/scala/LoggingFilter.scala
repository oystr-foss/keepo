import akka.stream.Materializer
import javax.inject.Inject
import play.api.Logger
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}


class LoggingFilter @Inject()(
    implicit val mat: Materializer,
    ec: ExecutionContext) extends Filter {
    val logger = Logger("access")

    def apply(nextFilter: RequestHeader => Future[Result])(requestHeader: RequestHeader): Future[Result] = {
        val startTime = System.currentTimeMillis
        nextFilter(requestHeader)
            .map { result =>
                val endTime = System.currentTimeMillis
                val requestTime = endTime - startTime

                logger.info(s"${requestHeader.method} ${requestHeader.uri} ${result.header.status} (${requestTime}ms)")
                result.withHeaders("Request-Time" -> requestTime.toString)
            }
    }
}