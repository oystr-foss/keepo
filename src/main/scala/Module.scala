import java.time.Clock

import com.google.inject.AbstractModule
import oystr.akka.{RootActors, RootActorsImpl}
import oystr.services.{BasicServices, Services}
import oystr.vaultclient.{SttpVaultClient, VaultClient}


class Module extends AbstractModule {
    override def configure(): Unit = {
        bind(classOf[Clock]).toInstance(Clock.systemDefaultZone)
        bind(classOf[Services]).to(classOf[BasicServices]).asEagerSingleton()
        bind(classOf[RootActors]).to(classOf[RootActorsImpl]).asEagerSingleton()
        bind(classOf[VaultClient]).to(classOf[SttpVaultClient])
    }
}
