package scala.meta.internal.builds

import java.io.File

import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.Promise
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import scala.util.Properties

import scala.meta.internal.metals.Cancelable
import scala.meta.internal.metals.JavaBinary
import scala.meta.internal.metals.JdkSources
import scala.meta.internal.metals.MetalsEnrichments._
import scala.meta.internal.metals.MutableCancelable
import scala.meta.internal.metals.SlowTask
import scala.meta.internal.metals.Time
import scala.meta.internal.metals.Timer
import scala.meta.internal.process.ExitCodes
import scala.meta.internal.process.SystemProcess
import scala.meta.io.AbsolutePath

import coursierapi._

class ShellRunner(time: Time, slowTaskProvider: SlowTask)(implicit
    executionContext: scala.concurrent.ExecutionContext
) extends Cancelable {

  private val cancelables = new MutableCancelable()

  override def cancel(): Unit = {
    cancelables.cancel()
  }

  def runJava(
      dependency: Dependency,
      main: String,
      dir: AbsolutePath,
      arguments: List[String],
      javaHome: Option[String],
      redirectErrorOutput: Boolean = false,
      processOut: String => Unit = scribe.info(_),
      processErr: String => Unit = scribe.error(_),
      propagateError: Boolean = false,
      javaOptsMap: Map[String, String] = Map.empty,
  ): Future[Int] = {

    val classpathSeparator = if (Properties.isWin) ";" else ":"
    val classpath = Fetch
      .create()
      .withDependencies(dependency)
      .withRepositories(ShellRunner.defaultRepositories: _*)
      .fetch()
      .asScala
      .mkString(classpathSeparator)

    val javaOpts = javaOptsMap.map { case (key, value) =>
      s"-D$key=$value"
    }.toList

    val cmd =
      JavaBinary(javaHome) ::
        javaOpts :::
        List(
          "-classpath",
          classpath,
          main,
        ) ::: arguments

    run(
      main,
      cmd,
      dir,
      redirectErrorOutput,
      javaHome,
      processOut = processOut,
      processErr = processErr,
      propagateError = propagateError,
    )
  }

  def run(
      commandRun: String,
      args: List[String],
      directory: AbsolutePath,
      redirectErrorOutput: Boolean,
      javaHome: Option[String],
      additionalEnv: Map[String, String] = Map.empty,
      processOut: String => Unit = scribe.info(_),
      processErr: String => Unit = scribe.error(_),
      propagateError: Boolean = false,
      logInfo: Boolean = true,
  ): Future[Int] = {
    val elapsed = new Timer(time)
    val env = additionalEnv ++ JdkSources.envVariables(javaHome)
    val ps = SystemProcess.run(
      args,
      directory,
      redirectErrorOutput,
      env,
      Some(processOut),
      Some(processErr),
      propagateError,
    )
    val result = Promise[Int]
    val newCancelable: Cancelable = () => ps.cancel
    cancelables.add(newCancelable)

    val processFuture = ps.complete
    slowTaskProvider.trackFuture(
      commandRun,
      processFuture,
      onCancel = Some(() => {
        result.trySuccess(ExitCodes.Cancel)
        ps.cancel
      }),
    )
    processFuture.map { code =>
      if (logInfo)
        scribe.info(s"time: ran '$commandRun' in $elapsed")
      result.trySuccess(code)
    }
    result.future.onComplete(_ => cancelables.remove(newCancelable))
    result.future
  }

}

object ShellRunner {

  private lazy val mavenLocal = {
    val str = new File(sys.props("user.home")).toURI.toString
    val homeUri =
      if (str.endsWith("/"))
        str
      else
        str + "/"
    MavenRepository.of(homeUri + ".m2/repository")
  }

  private lazy val sonatypePublic = MavenRepository.of(
    "https://oss.sonatype.org/content/repositories/public"
  )

  val defaultRepositories: List[Repository] =
    List(
      Repository.ivy2Local(),
      Repository.central(),
      mavenLocal,
      sonatypePublic,
    )

  def runSync(
      args: List[String],
      directory: AbsolutePath,
      redirectErrorOutput: Boolean,
      additionalEnv: Map[String, String] = Map.empty,
      processErr: String => Unit = scribe.error(_),
      propagateError: Boolean = false,
      maybeJavaHome: Option[String] = None,
  )(implicit ec: ExecutionContext): Option[String] = {

    val sbOut = new StringBuilder()
    val env = additionalEnv ++ maybeJavaHome.map("JAVA_HOME" -> _).toMap
    val ps = SystemProcess.run(
      args,
      directory,
      redirectErrorOutput,
      env,
      Some(s => {
        sbOut.append(s)
        sbOut.append(Properties.lineSeparator)
      }),
      Some(processErr),
      propagateError,
    )

    val exit = Await.result(ps.complete, 10 second)

    if (exit == 0) {
      Some(sbOut.toString())
    } else None
  }
}
