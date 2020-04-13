package oystr.domain

import play.api.libs.json.Json

case class Done(status: Boolean = true)
case class Error(message: String)
case class Policy(policy: String)
case class TokenRequest(ttl: String, policies: Seq[String], meta: TokenMetadata, renewable: Boolean)
case class TokenMetadata(organization: String, name: String)

case class Metadata(organization: String, name: Option[String], bot: Option[String], token: Option[String])
case class Content(username: String, password: Option[String], certificate: Option[String])
case class StoreDataRequest(metadata: Metadata, content: Content)

sealed trait Secrets
case class Credentials(username: String, password: String) extends Secrets
case class CredentialsRequest(data: Secrets)
case class Certificate(username: String, certificate: String) extends Secrets

object json {
    implicit val MetadataWriter = Json.writes[Metadata]
    implicit val MetadataReader = Json.reads[Metadata]
    implicit val ContentWriter = Json.writes[Content]
    implicit val ContentReader = Json.reads[Content]

    implicit val DoneWriter = Json.writes[Done]
    implicit val DoneReader = Json.reads[Done]
    implicit val ErrorWriter = Json.writes[Error]
    implicit val ErrorReader = Json.reads[Error]
    implicit val PolicyWriter = Json.writes[Policy]
    implicit val PolicyReader = Json.reads[Policy]
    implicit val TokenMetadataWriter = Json.writes[TokenMetadata]
    implicit val TokenMetadataReader = Json.reads[TokenMetadata]
    implicit val TokenRequestWriter = Json.writes[TokenRequest]
    implicit val TokenRequestReader = Json.reads[TokenRequest]

    implicit val CredentialsWriter = Json.writes[Credentials]
    implicit val CredentialsReader = Json.reads[Credentials]
    implicit val CertificateWriter = Json.writes[Certificate]
    implicit val CertificateReader = Json.reads[Certificate]
    implicit val SecretsWriter = Json.writes[Secrets]
    implicit val SecretsReader = Json.reads[Secrets]
    implicit val CredentialsRequestWriter = Json.writes[CredentialsRequest]
    implicit val CredentialsRequestReader = Json.reads[CredentialsRequest]

    implicit val StoreDataRequestWriter = Json.writes[StoreDataRequest]
    implicit val StoreDataRequestReads = Json.reads[StoreDataRequest]
}