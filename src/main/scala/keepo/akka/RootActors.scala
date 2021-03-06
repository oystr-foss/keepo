package keepo.akka

import akka.actor.{ActorRef, Props}
import javax.inject.{Inject, Singleton}
import keepo.services.{BasicServices, VaultActor}
import keepo.vaultclient.SttpVaultClient
import play.api.Configuration

import scala.concurrent.ExecutionContext


trait RootActors {
    def vault: ActorRef
}

@Singleton
class RootActorsImpl @Inject() (
    services: BasicServices,
    conf: Configuration,
    client: SttpVaultClient,
    executionContext: ExecutionContext) extends RootActors {

    private val vaultActor = services
        .sys()
        .actorOf(
            Props(
                classOf[VaultActor],
                services,
                client
            ), "vault-service")

    override def vault: ActorRef = vaultActor
}
