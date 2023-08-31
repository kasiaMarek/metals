package tests

import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

import scala.reflect.internal.util.BatchSourceFile
import scala.tools.nsc
import scala.tools.nsc.reporters.StoreReporter

import scala.meta.internal.implementation.ClassGraphIndex
import scala.meta.internal.mtags.MtagsEnrichments._
import scala.meta.io.AbsolutePath

class TypeHierarchyScala2Suite()
    extends SingleFileExpectSuite("type-hierarchy.expect") {
  override lazy val input: InputProperties = InputProperties.scala2()
  lazy val workspace: Path = Paths
    .get(".")
    .toAbsolutePath
    .resolve("target/type-hierarchy/")
    .createDirectories()
  val reporter = new StoreReporter
  def myClasspath: Seq[Path] =
    System
      .getProperty("java.class.path")
      .split(java.io.File.pathSeparator)
      .map(Paths.get(_))
      .toSeq
  lazy val out: AbsolutePath = AbsolutePath(workspace.resolve("out.jar"))
  lazy val g: nsc.Global = {
    val settings = new nsc.Settings()
    settings.classpath.value =
      myClasspath.map(_.toString).mkString(File.pathSeparator)
    settings.Yrangepos.value = true
    settings.d.value = out.toString
    new nsc.Global(settings, reporter)
  }
  val excludedFiles: Set[String] =
    Set("Definitions.scala", "MacroAnnotation.scala")
  override def obtained(): String = {
    val run = new g.Run()
    val sources =
      input.allFiles.collect {
        case file if !excludedFiles(file.file.toNIO.filename) =>
          new BatchSourceFile(file.input.path, file.input.text.toCharArray())
      }
    run.compileSources(sources)
    ClassGraphIndex
      .inheritanceMap(
        out.toNIO :: myClasspath.toList,
        _.acceptPackages("example").enableExternalClasses(),
      )
      .collect {
        case (inheritee, inherited) if inherited.nonEmpty =>
          s"$inheritee -> ${inherited.mkString(", ")}"
      }
      .mkString("\n")
  }
}
