package tests

import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContextExecutorService
import scala.concurrent.Future
import scala.concurrent.Promise
import scala.concurrent.duration.FiniteDuration

import scala.meta.internal.metals.Messages
import scala.meta.internal.metals.MetalsEnrichments._
import scala.meta.internal.metals.clients.language.NoopLanguageClient
import scala.meta.internal.metals.utils.RequestRegistry
import scala.meta.internal.metals.utils.Timeout

import munit.FunSuite
import org.eclipse.lsp4j.MessageActionItem
import org.eclipse.lsp4j.ShowMessageRequestParams

class RequestRegistrySuite extends FunSuite {
  implicit val ex: ExecutionContextExecutorService =
    ExecutionContext.fromExecutorService(Executors.newCachedThreadPool())
  val duration: FiniteDuration = FiniteDuration(1, TimeUnit.SECONDS)
  def createRegistry() =
    new RequestRegistry(duration, List(), CancellingLanguageClient)
  val defaultTimeout: Timeout.DefaultFlexTimeout =
    Timeout.DefaultFlexTimeout("request")

  test("avg") {
    val requestRegistry = createRegistry()
    val doneTimeout = Timeout.FlexTimeout("done", duration)
    for {
      _ <- requestRegistry
        .register(
          ExampleFutures.done,
          doneTimeout,
        )
        .future
      _ <- requestRegistry
        .register(
          ExampleFutures.done,
          doneTimeout,
        )
        .future
      _ <- requestRegistry
        .register(
          ExampleFutures.done,
          doneTimeout,
        )
        .future
      _ <- requestRegistry.register(ExampleFutures.fast, defaultTimeout).future
      _ <- requestRegistry.register(ExampleFutures.fast, defaultTimeout).future
      _ <- requestRegistry.register(ExampleFutures.fast, defaultTimeout).future
      _ = assert(
        requestRegistry.getTimeout(defaultTimeout).get > duration
      )
      _ = assert(
        requestRegistry
          .getTimeout(defaultTimeout)
          .get < duration * 2
      )
      _ = assert(requestRegistry.getTimeout(doneTimeout).get == duration)
    } yield ()
  }

  test("timeout") {
    val promise = Promise[Unit]()
    val requestRegistry = createRegistry()
    for {
      err <- requestRegistry
        .register(ExampleFutures.infinite(promise), defaultTimeout)
        .future
        .failed
      _ = assert(err.isInstanceOf[TimeoutException])
      _ = assertEquals(
        requestRegistry.getTimeout(defaultTimeout).get,
        duration * 3,
      )
      _ <- requestRegistry.register(ExampleFutures.fast, defaultTimeout).future
      _ <- promise.future
    } yield ()
  }

  test("cancel") {
    val promise1 = Promise[Unit]()
    val promise2 = Promise[Unit]()
    val requestRegistry = createRegistry()
    val f1 = requestRegistry.register(
      ExampleFutures.infinite(promise1),
      Timeout.NoTimeout
    )
    val f2 = requestRegistry.register(
      ExampleFutures.infinite(promise2),
      Timeout.NoTimeout
    )
    requestRegistry.cancel()
    for {
      err <- f1.future.failed
      _ = assert(err.isInstanceOf[CancellationException])
      err2 <- f2.future.failed
      _ = assert(err2.isInstanceOf[CancellationException])
      _ <- promise1.future
      _ <- promise2.future
    } yield ()
  }

}

object RequestType extends Enumeration {
  val Type1, Type2 = Value
}

object CancellingLanguageClient extends NoopLanguageClient {
  override def showMessageRequest(
      requestParams: ShowMessageRequestParams
  ): CompletableFuture[MessageActionItem] =
    Future.successful(Messages.RequestTimeout.cancel).asJava
}
