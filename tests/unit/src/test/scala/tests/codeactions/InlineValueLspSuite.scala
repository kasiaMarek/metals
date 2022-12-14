package tests.codeactions

import scala.meta.internal.metals.codeactions.InlineValueCodeAction
import scala.meta.internal.metals.codeactions.ExtractRenameMember

class InlineValueLspSuite extends BaseCodeActionLspSuite("inlineValueRewrite") {
  check(
    "one-use-of-the-value",
    """|object Main {
       | def u : Unit = {
       |  val l : List[Char] = List(1)
       |  def m(i : Int) : Int = ???
       |  def get(): Unit = <<l>>.map(x => m(x))
       | }
       |}
       |""".stripMargin,
    s"""|${InlineValueCodeAction.title("l")}""".stripMargin,
    """|object Main {
       | def u : Unit = {
       |  def m(i : Int) : Int = ???
       |  def get(): Unit = List(1).map(x => m(x))
       | }
       |}
       |""".stripMargin,
    fileName = "Main.scala",
  )

  check(
    "multiple-uses-with-backticks",
    """|object Main {
       |  val `l` : List[Int] = List(1) ++ List(2)
       |  def m(i : Int) : Int = ???
       |  def get(): Unit = `<<l>>`.map(x => m(x))
       |  def get2(): Unit = `l`.map(x => m(x))
       |}
       |""".stripMargin,
    s"""|${InlineValueCodeAction.title("l")}""".stripMargin,
    """|object Main {
       |  val `l` : List[Int] = List(1) ++ List(2)
       |  def m(i : Int) : Int = ???
       |  def get(): Unit = (List(1) ++ List(2)).map(x => m(x))
       |  def get2(): Unit = `l`.map(x => m(x))
       |}
       |""".stripMargin,
    fileName = "Main.scala",
  )

  check(
    "multiple-uses-of-the-values",
    """|object Main {
       |  val l : List[Char] = List(1)
       |  def m(i : Int) : Int = ???
       |  def get(): Unit = <<l>>.map(x => m(x))
       |  def get2(): Unit = l.map(x => m(x))
       |}
       |""".stripMargin,
    s"""|${InlineValueCodeAction.title("l")}""".stripMargin,
    """|object Main {
       |  val l : List[Char] = List(1)
       |  def m(i : Int) : Int = ???
       |  def get(): Unit = List(1).map(x => m(x))
       |  def get2(): Unit = l.map(x => m(x))
       |}
       |""".stripMargin,
    fileName = "Main.scala",
  )

  checkNoAction(
    "should-not-inline-if-value-has-mod-override",
    """|abstract class HasValV {
       |  val v : Int
       |}
       |object Main extends HasValV {
       |  override val <<v>> : Int = 1
       |  def someF(x : Int): Int = x + v + 3
       |}
       |""".stripMargin,
    fileName = "Main.scala",
    filterAction =
      _.getTitle != s"""${ExtractRenameMember.title("class", "HasValV")}""",
  )

  check(
    "should-not-delete-if-value-overrides",
    """|abstract class HasValV {
       |  val v : Int
       |}
       |object Main extends HasValV {
       |  override val v : Int = 1
       |  def someF(x : Int): Int = x + <<v>> + 3
       |}
       |""".stripMargin,
    s"""|${InlineValueCodeAction.title("v")}""".stripMargin,
    """|abstract class HasValV {
       |  val v : Int
       |}
       |object Main extends HasValV {
       |  override val v : Int = 1
       |  def someF(x : Int): Int = x + 1 + 3
       |}
       |""".stripMargin,
    fileName = "Main.scala",
    filterAction =
      _.getTitle != s"""${ExtractRenameMember.title("class", "HasValV")}""",
  )

  check(
    "check-pos-on-def",
    """|object Main {
       |  val m: Int = {
       |    val <<a>>: Option[Int] = Some(1)
       |    a match {
       |       case _ => ???
       |    }
       |  }
       |}""".stripMargin,
    s"""|${InlineValueCodeAction.title("a")}""".stripMargin,
    """|object Main {
       |  val m: Int = {
       |    Some(1) match {
       |       case _ => ???
       |    }
       |  }
       |}""".stripMargin,
    fileName = "Main.scala",
  )

  check(
    "check-adds-brackets",
    """|object Main {
       |  val p : Int = 2
       |  val r : Int = p - 1
       |  val s : Int = s - <<r>>
       |}""".stripMargin,
    s"""|${InlineValueCodeAction.title("r")}""".stripMargin,
    """|object Main {
       |  val p : Int = 2
       |  val r : Int = p - 1
       |  val s : Int = s - (p - 1)
       |}""".stripMargin,
    fileName = "Main.scala",
  )

  checkNoAction(
    "check-no-inline-when-not-local",
    """|object Main {
       |  val p : Int = 2
       |  val <<r>> : Int = p - 1
       |  val s : Int = s - r
       |}""".stripMargin,
    fileName = "Main.scala",
  )
}
