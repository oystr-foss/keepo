package oystr

import org.apache.commons.lang3.StringUtils
import play.api.Configuration
import play.api.mvc.Request

object Utils {
    implicit class TokenHelper(request: Request[AnyRef]) {
        def vaultToken()(implicit configuration: Configuration): Option[String] = extract("vault")
        def morbidToken()(implicit configuration: Configuration): Option[String] = extract("morbid")

        def validate(requireVaultToken: Boolean)(implicit configuration: Configuration): Boolean = {
            morbidToken().isDefined && ((requireVaultToken && vaultToken().isDefined) || !requireVaultToken)
        }

        private def extract(key: String)(implicit configuration: Configuration): Option[String] = {
            val header = configuration.get[String](s"$key.header")
            request.headers.get(header).filter(StringUtils.isNotEmpty)
        }
    }
}