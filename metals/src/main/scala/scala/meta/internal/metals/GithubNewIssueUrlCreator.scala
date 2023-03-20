package scala.meta.internal.metals

import java.net.URLEncoder

import scala.util.Properties

import scala.meta.internal.bsp.BspResolvedResult
import scala.meta.internal.bsp.BspSession
import scala.meta.internal.bsp.ResolvedBloop
import scala.meta.internal.bsp.ResolvedBspOne
import scala.meta.internal.bsp.ResolvedMultiple
import scala.meta.internal.bsp.ResolvedNone
import scala.meta.internal.builds.BuildTools

import org.eclipse.lsp4j.ClientInfo

class GithubNewIssueUrlCreator(
    folderInfo: List[GitHubIssueFolderInfo],
    clientInfo: ClientInfo,
) {

  def buildUrl(): String = {
    val scalaVersions =
      folderInfo
        .flatMap(_.buildTargets.allScala)
        .map(_.scalaVersion)
        .toSet
        .mkString("; ")
    val clientVersion =
      Option(clientInfo.getVersion()).map(v => s" v$v").getOrElse("")
    val body =
      s"""|<!--
          |        Describe the bug ...
          |
          |        Reproduction steps
          |          1. Go to ...
          |          2. Click on ...
          |          3. Scroll down to ...
          |          4. See error
          |-->
          |
          |### Expected behaviour:
          |
          |<!-- A clear and concise description of what you expected to happen. -->
          |
          |**Operating system:**
          |${Properties.osName}
          |
          |**Java version:**
          |${Properties.javaVersion}
          |
          |**Editor/extension:**
          |${clientInfo.getName()}$clientVersion
          |
          |**Metals version:**
          |${BuildInfo.metalsVersion}
          |
          |### Extra context or search terms:
          |<!--
          |        - Any other context about the problem
          |        - Search terms to help others discover this
          |-->
          |
          |### Workspace information:
          | - **Scala versions:** $scalaVersions$selectedBuildTool$selectedBuildServer
          | - **All build tools in workspace:** ${folderInfo.flatMap(_.buildTools.all).mkString("; ")}
          |""".stripMargin
    s"https://github.com/scalameta/metals/issues/new?body=${URLEncoder.encode(body)}"
  }

  private def selectedBuildTool(): String = {
    val buildTools =
      folderInfo.map { info =>
        info.tables.buildTool
          .selectedBuildTool()
      }

    if (buildTools.find(_.isDefined).isDefined) {
      val value = buildTools.zipWithIndex
        .map { case (buildTool, indx) =>
          s"$indx. $buildTool"
        }
        .mkString("\n    ")
      s"""|
          | - **Build tools:** ${value}""".stripMargin
    } else ""
  }

  private def selectedBuildServer(): String = {
    val buildServers =
      folderInfo.zipWithIndex.map { case (info, indx) =>
        import info._
        val buildServer = currentBuildServer()
          .map(s => s"${s.main.name} v${s.main.version}")
          .getOrElse {
            calculateNewBuildServer() match {
              case ResolvedBloop => "Disconnected: Bloop"
              case ResolvedBspOne(details) =>
                s"Disconnected: ${details.getName()}"
              case ResolvedMultiple(_, details) =>
                s"Disconnected: Multiple Found ${details.map(_.getName()).mkString("; ")}"
              case ResolvedNone => s"Disconnected: None Found"
            }
          }
        s"$indx. $buildServer"
      }
    s"""|
        | - **Build servers:**
        |    ${buildServers.mkString("\n    ")}""".stripMargin
  }
}

case class GitHubIssueFolderInfo(
    tables: Tables,
    buildTargets: BuildTargets,
    currentBuildServer: () => Option[BspSession],
    calculateNewBuildServer: () => BspResolvedResult,
    buildTools: BuildTools,
)
