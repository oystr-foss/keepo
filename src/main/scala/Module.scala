import java.time.Clock

import com.google.inject.AbstractModule
import keepo.akka.{RootActors, RootActorsImpl}
import keepo.services.{BasicServices, Services}
import keepo.vaultclient.{SttpVaultClient, VaultClient}


class Module extends AbstractModule {
    override def configure(): Unit = {
        bind(classOf[Clock]).toInstance(Clock.systemDefaultZone)
        bind(classOf[Services]).to(classOf[BasicServices]).asEagerSingleton()
        bind(classOf[RootActors]).to(classOf[RootActorsImpl]).asEagerSingleton()
        bind(classOf[VaultClient]).to(classOf[SttpVaultClient])
    }
}
