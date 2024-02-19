package tests

import scala.meta.internal.io.PathIO
import scala.meta.internal.metals.Buffers
import scala.meta.internal.metals.StatusBar

class StatusBarSuite extends BaseSuite {
  val time = new FakeTime
  val client = new TestingClient(PathIO.workingDirectory, Buffers())
  var status = new StatusBar(client, time)
  override def beforeEach(context: BeforeEach): Unit = {
    client.statusParams.clear()
    status.cancel()
  }

  def tickSecond(): Unit = {
    time.elapseSeconds(1)
    status.tick()
  }

  test("message") {
    status.addMessage("tick 1")
    time.elapseSeconds(5)
    status.addMessage("tick 2")
    status.tick()
    time.elapseSeconds(11)
    status.tick()
    assertNoDiff(
      client.statusBarHistory,
      """|
         |<show> - tick 1
         |tick 2
         |<hide>
         |""".stripMargin,
    )
  }
}
