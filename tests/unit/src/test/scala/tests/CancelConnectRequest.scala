package scala.meta.internal.metals.tests

import scala.meta.internal.metals.Messages.ImportBuild
import scala.meta.internal.metals.ServerCommands
import scala.meta.internal.metals.{BuildInfo => V}

import org.eclipse.lsp4j.MessageActionItem
import tests.BaseLspSuite
import tests.BloopImportInitializer

class CancelConnectRequest
    extends BaseLspSuite("cancel-connect-request", BloopImportInitializer) {

  test("switch-build-server-while-connect") {
    cleanWorkspace()
    val layout =
      s"""|/project/build.properties
          |sbt.version=${V.sbtVersion}
          |/build.sbt
          |scalaVersion := "${V.scala213}"
          |/src/main/scala/A.scala
          |
          |object A {
          |  val i: Int = "aaa"
          |}
          |""".stripMargin
    writeLayout(layout)
    client.importBuild = ImportBuild.yes
    client.selectBspServer = { _ => new MessageActionItem("sbt") }
    for {
      _ <- server.initialize()
      _ = server.initialized()
      _ = while (server.server.connect.onGoingConnect.get().isCompleted()) {
        // wait for connect to start
        Thread.sleep(100)
      }
      bloopConnectF = server.server.connect.onGoingConnect.get().future
      bspSwitchF = server.executeCommand(ServerCommands.BspSwitch)
      _ <- bloopConnectF
      _ = assert(!server.server.indexingPromise.isCompleted)
      _ <- bspSwitchF
      _ = assert(server.server.indexingPromise.isCompleted)
      _ = assert(server.server.bspSession.exists(_.main.isSbt))
    } yield ()
  }
}
