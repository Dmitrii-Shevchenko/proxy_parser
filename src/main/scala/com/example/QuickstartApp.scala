package com.example

import akka.actor.typed.{ActorSystem, Terminated}
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import com.example.controllers.{ProxyController, UserController}
import com.example.services.{ProxyService, UserRegistry}

import scala.util.Failure
import scala.util.Success

object QuickstartApp {
  private def startHttpServer(routes: Route)(implicit system: ActorSystem[_]): Unit = {
    import system.executionContext

    val futureBinding = Http().newServerAt("localhost", 8080).bind(routes)
    futureBinding.onComplete {
      case Success(binding) =>
        val address = binding.localAddress
        system.log.info("Server online at http://{}:{}/", address.getHostString, address.getPort)
      case Failure(ex) =>
        system.log.error("Failed to bind HTTP endpoint, terminating system", ex)
        system.terminate()
    }
  }

  def main(args: Array[String]): Unit = {
    val rootBehavior = Behaviors.setup[Nothing] { context =>
      val userRegistryActor = context.spawn(UserRegistry(), "UserRegistryActor")
      /*
      The default behavior for the parent actor is to fail itself, bubbling the failure up the hierarchy.
      However, what if a parent wants to remain alive in case one of its child actors stops? First of all,
      the parent actor must watch its child:
       */
      context.watch(userRegistryActor)
      /*
      Watching an actor means to receive a ChildFailed signal when it stops. ChildFailed extends the more general signal Terminated:
      .receiveSignal {
      case (ctx, ChildFailed(ref, cause)) =>
        ctx.log.error(s"Child actor ${ref.path} failed with error ${cause.getMessage}")
        Behaviors.same
      }
      However, if the parent actor decides to not handle the ChildFailed signal,
      it will terminate itself, raising a DeathPactException, thereby bubbling up the failure.
      */

      val userRoute = new UserController(userRegistryActor)(context.system).route
      val proxyRoute = new ProxyController(new ProxyService()(context.system)).route

      val routes = userRoute ~ proxyRoute
      startHttpServer(routes)(context.system)

      Behaviors.receiveSignal[Nothing] {
        case (context, Terminated(ref)) =>
          context.log.info("Actors stopped: {}", ref.path.name)
          Behaviors.stopped
      }
    }
    // The top level actor, also called the user guardian actor, is created along with the ActorSystem
    val system = ActorSystem[Nothing](rootBehavior, "HelloAkkaHttpServer")
  }
}
