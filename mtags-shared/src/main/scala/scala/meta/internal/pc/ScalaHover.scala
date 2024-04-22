package scala.meta.internal.pc

import java.util.Optional

import scala.meta.internal.mtags.CommonMtagsEnrichments._
import scala.meta.pc.ContentType
import scala.meta.pc.ContentType.MARKDOWN
import scala.meta.pc.HoverSignature

import org.eclipse.lsp4j

case class ScalaHover(
    expressionType: Option[String] = None,
    symbolSignature: Option[String] = None,
    docstring: Option[String] = None,
    forceExpressionType: Boolean = false,
    range: Option[lsp4j.Range] = None,
    contextInfo: List[String], // e.g. info about rename imports
    override val contentType: ContentType
) extends HoverSignature {

  def this(
      expressionType: Option[String],
      symbolSignature: Option[String],
      docstring: Option[String],
      forceExpressionType: Boolean,
      range: Option[lsp4j.Range],
      contextInfo: List[String]
  ) =
    this(
      expressionType,
      symbolSignature,
      docstring,
      forceExpressionType,
      range,
      contextInfo,
      contentType = ContentType.MARKDOWN
    )

  def signature(): Optional[String] = symbolSignature.asJava

  def toLsp(): lsp4j.Hover = {
    val markup =
      HoverMarkup(
        expressionType.getOrElse(""),
        symbolSignature,
        docstring.getOrElse(""),
        forceExpressionType,
        contextInfo,
        markdown = contentType == MARKDOWN
      )
    new lsp4j.Hover(markup.toMarkupContent(contentType), range.orNull)
  }

  def getRange(): Optional[lsp4j.Range] = range.asJava

  def withRange(range: lsp4j.Range): HoverSignature =
    ScalaHover(
      expressionType,
      symbolSignature,
      docstring,
      forceExpressionType,
      Some(range),
      contextInfo,
      contentType
    )

}
