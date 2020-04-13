package oystr.services

import java.time.Clock

import akka.actor.ActorSystem
import javax.inject.{Inject, Singleton}
import play.api.{Configuration, Environment}

import scala.concurrent.ExecutionContext


trait Services {
    def conf(): Configuration
    def env(): Environment
    def clock(): Clock
    def sys(): ActorSystem
    def ec(): ExecutionContext
}

@Singleton
class BasicServices @Inject() (
    configuration: Configuration,
    environment: Environment,
    theClock: Clock,
    system: ActorSystem,
    executionContext: ExecutionContext) extends Services {

    override def conf() = configuration
    override def env() = environment
    override def clock() = theClock
    override def sys() = system
    override def ec() = executionContext
}
