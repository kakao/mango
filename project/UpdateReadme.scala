import java.nio.charset.StandardCharsets._
import java.nio.file.Files._
import java.nio.file.Paths

import sbt.State

import sbtrelease.ReleasePlugin.autoImport._
import scala.collection.JavaConversions._
import sys.process._

/** A script that automatically updates Mango's version string in the example in README.md */
object UpdateReadme {

  val subprojects = Seq("mango", "mango-core")

  /** update the library versions in the examples in README.md; this function is called right before commiting the release version */
  def updateReadme(state: State): State = {
    // version.sbt has the version that we are releasing
    val revision = "\"([^\"]+)\"".r.findFirstMatchIn(readAllLines(Paths.get("version.sbt"), UTF_8).mkString("")).get.group(1)

    // read all lines from README.md
    val lines = readAllLines(Paths.get("README.md"), UTF_8)

    // the examples should be between these two markers
    val start = lines.indexWhere(_.startsWith("<!-- DO NOT EDIT: The section below"))
    val end = lines.indexWhere(_.startsWith("<!-- DO NOT EDIT: The section above"))

    if (start == -1 || end == -1) {
      throw new RuntimeException("Could not find markers on README.md")
    }

    val before = lines.take(start + 2)
    val after = lines.takeRight(lines.size - end + 1)

    System.setProperty("aa.banana.configuration", "")
    write(Paths.get("README.md"), before ++ subprojects.map {
      p => s"""libraryDependencies += "com.kakao.mango" %% "$p" % "$revision""""
    } ++ after, UTF_8)

    "git add README.md"!

    state
  }

  /** a ReleaseStep containing updateReadme is added to the default release process */
  val customReleaseProcess = {
    import ReleaseTransformations._
    Seq[ReleaseStep](
      checkSnapshotDependencies, inquireVersions, runTest, setReleaseVersion,
      ReleaseStep(action = updateReadme, enableCrossBuild = true),
      commitReleaseVersion, tagRelease, publishArtifacts, setNextVersion, commitNextVersion, pushChanges
    )
  }

}
