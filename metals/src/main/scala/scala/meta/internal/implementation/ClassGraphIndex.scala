package scala.meta.internal.implementation

import java.nio.file.Path

import scala.meta.internal.metals.MetalsEnrichments._

import io.github.classgraph.ClassGraph

object ClassGraphIndex {

  def inheritanceMap(
      classpath: Seq[Path],
      transform: ClassGraph => ClassGraph = identity,
  ): Map[String, List[String]] = {
    val basicClassGraph = new ClassGraph()
      .overrideClasspath(classpath.asJava)
      .enableInterClassDependencies()
    val enrichedClassGraph = transform(basicClassGraph)
    val scanResult = enrichedClassGraph.scan()
    val res = scanResult
      .getClassDependencyMap()
      .asScala
      .map { case (classInfo, classInfoList) =>
        classInfo.getName() -> classInfoList.getNames().asScala.toList
      }
      .toMap
    scanResult.close()
    res
  }

}
