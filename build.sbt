import com.typesafe.sbt.pgp.PgpKeys.{publishSigned, publishLocalSigned}

import scalariform.formatter.preferences._

// Settings for entire build

ThisBuild/version := "2.3.0-a8"

ThisBuild/organization := "org.bitbucket.inkytonik.kiama"

ThisBuild/scalaVersion := "2.12.10"
ThisBuild/crossScalaVersions := Seq("2.13.0", "2.12.10")

ThisBuild/scalacOptions := {
    // Turn on all lint warnings, except:
    //  - stars-align: incorrectly reports problems if pattern matching of
    //    unapplySeq extractor doesn't match sequence directly
    val lintOption =
        if (scalaVersion.value.startsWith("2.10"))
            "-Xlint"
        else if (scalaVersion.value.startsWith("2.13"))
            "-Xlint:-stars-align,-nonlocal-return,_"
        else
            "-Xlint:-stars-align,_"
    Seq(
        "-deprecation",
        "-feature",
        "-sourcepath", baseDirectory.value.getAbsolutePath,
        "-unchecked",
        "-Xcheckinit",
        "-Xfatal-warnings",
        lintOption
    )
}

ThisBuild/resolvers ++=
    Seq(
        "Accur8 Repo" at "https://locus.accur8.io/repos/all",
        Resolver.sonatypeRepo("releases"),
        Resolver.sonatypeRepo("snapshots")
    )

ThisBuild/credentials += Credentials(Path.userHome / ".sbt" / "credentials")
ThisBuild/credentials += Credentials(Path.userHome / ".sbt" / "credentials.reader")

ThisBuild/incOptions := (ThisBuild/incOptions).value.withLogRecompileOnMacro(false)

ThisBuild/logLevel := Level.Info

ThisBuild/shellPrompt := {
    state =>
        Project.extract(state).currentRef.project + " " + version.value +
            " " + scalaVersion.value + "> "
}

ThisBuild/mainClass := None

// Common project settings

val commonSettings =
    Seq(
        unmanagedSourceDirectories in Compile ++= {
            val sourceDir = (sourceDirectory in Compile).value
            CrossVersion.partialVersion(scalaVersion.value) match {
                case Some((2, 10)) =>
                    Seq(sourceDir / "scala-2.10", sourceDir / "scala-2.not11", sourceDir / "scala-2.12-")
                case Some((2, 11)) =>
                    Seq(sourceDir / "scala-2.11", sourceDir / "scala-2.11+", sourceDir / "scala-2.12-")
                case Some((2, 12)) =>
                    Seq(sourceDir / "scala-2.not11", sourceDir / "scala-2.11+", sourceDir / "scala-2.12-")
                case Some((2, 13)) =>
                    Seq(sourceDir / "scala-2.not11", sourceDir / "scala-2.11+", sourceDir / "scala-2.13")
                case version =>
                    sys.error(s"unexpected Scala version $version")
            }
        },

        libraryDependencies ++=
            Seq(
                "org.scalacheck" %% "scalacheck" % "1.14.3" % "test",
                "org.scalatest" %% "scalatest" % "3.1.0" % "test",
                "org.scalatestplus" %% "scalacheck-1-14" % "3.1.0.0" % "test"
            ),

        // Formatting
        scalariformPreferences := scalariformPreferences.value
            .setPreference(AlignSingleLineCaseStatements, true)
            .setPreference(DanglingCloseParenthesis, Force)
            .setPreference(IndentSpaces, 4)
            .setPreference(SpaceBeforeColon, true)
            .setPreference(SpacesAroundMultiImports, false),

        // Publishing
//        publishTo := {
//            val nexus = "https://oss.sonatype.org/"
//            if (version.value.trim.endsWith("SNAPSHOT"))
//                Some("snapshots" at nexus + "content/repositories/snapshots")
//            else
//                Some("releases" at nexus + "service/local/staging/deploy/maven2")
//        },
        publishTo := Some("Accur8 Repo Publish" at "https://locus-beta.accur8.io/repos/releases"),
        publishMavenStyle := true,
        Test/publishArtifact := true,
        pomIncludeRepository := { _ => false },
        pomExtra := (
            <url>https://bitbucket.org/inkytonik/kiama</url>
            <licenses>
                <license>
                    <name>Mozilla Public License, v. 2.0</name>
                    <url>http://mozilla.org/MPL/2.0/</url>
                    <distribution>repo</distribution>
                </license>
            </licenses>
            <scm>
                <url>https://bitbucket.org/inkytonik/kiama</url>
                <connection>scm:hg:https://bitbucket.org/inkytonik/kiama</connection>
            </scm>
            <developers>
                <developer>
                   <id>inkytonik</id>
                   <name>Tony Sloane</name>
                   <url>https://bitbucket.org/inkytonik</url>
                </developer>
            </developers>
        )
    )

val versionSettings =
    Seq(
        libraryDependencies ++= {
            CrossVersion.partialVersion(scalaVersion.value) match {
                case Some((2, 10)) =>
                    // Avoids "Class javax.annotation.Nullable not found - continuing with a stub."
                    // and similar with 2.10 compiler
                    Seq("com.google.code.findbugs" % "jsr305" % "3.0.2")
                case _ =>
                    Seq()
            }
        }
    )

val scalaJSVersionOpt = Option(System.getProperty("scalaJSVersion"))

// Project configuration:
//   - base project containing macros and code that they need
//   - core project containing main Kiama functionality, including its tests
//   - extras project containing utilities, including their tests and examples
//   - kiama (root) project aggregates base, core and extras

def setupProject(project : Project, projectName : String) : Project =
    project.settings(
        name := projectName
    )

def setupSubProject(project : Project, projectName : String) : Project =
    setupProject(
        project,
        projectName
    ).enablePlugins(
        Seq(ScalaUnidocPlugin) ++ scalaJSVersionOpt.map(_ => ScalaJSPlugin).toSeq: _*
    ).settings(
        commonSettings : _*
    ).settings(
        versionSettings : _*
    )

val noPublishSettings =
    Seq(
        publish := {},
        publishLocal := {},
        publishSigned := {},
        publishLocalSigned := {}
    )

lazy val base =
    setupSubProject(
        project in file("base"),
        "base"
    ).settings(
        noPublishSettings : _*
    ).settings(
        libraryDependencies ++= Seq(
          // Caching:
          "com.google.guava" % "guava" % "21.0",
          // DSL support:
          "org.bitbucket.inkytonik.dsinfo" %%% "dsinfo" % "0.4.0-a8",
          // Profiling:
          "org.bitbucket.inkytonik.dsprofile" %%% "dsprofile" % "0.4.0-a8",
          // Reflection
          "org.scala-lang" % "scala-reflect" % scalaVersion.value
        ),
        //jsDependencyManifest in Compile := new File("") // uncomment this line for scala.js 0.6
    )

val extrasProject = ProjectRef(file("."), "extras")

lazy val core =
    setupSubProject(
        project in file("core"),
        "kiama"
    ).settings(
        libraryDependencies ++= Seq(
          // Caching:
          "com.google.guava" % "guava" % "21.0",
          // DSL support:
          "org.bitbucket.inkytonik.dsinfo" %%% "dsinfo" % "0.4.0-a8",
          // Profiling:
          "org.bitbucket.inkytonik.dsprofile" %%% "dsprofile" % "0.4.0-a8",
          // Reflection
          "org.scala-lang" % "scala-reflect" % scalaVersion.value
        ),

        console/initialCommands := """
            import org.bitbucket.inkytonik.kiama._
            import rewriting.Rewriter._
        """.stripMargin,
        Compile/packageBin/mappings := (Compile/packageBin/mappings).value ++ (base/Compile/packageBin/mappings).value,
        Compile/packageSrc/mappings := (Compile/packageSrc/mappings).value ++ (base/Compile/packageSrc/mappings).value,

        // Unidoc so we combine docs from base and core (but not extras)
        Compile/doc := (ScalaUnidoc/doc).value,
        Test/doc := (TestScalaUnidoc/doc).value,
        ScalaUnidoc/unidoc/target := crossTarget.value / "api",
        TestScalaUnidoc/unidoc/target := crossTarget.value / "test-api",
        ScalaUnidoc/unidoc/scalacOptions ++=
            Seq(
                if (scalaVersion.value.startsWith("2.10"))
                    "-Ymacro-no-expand"
                else
                    "-Ymacro-expand:none",
                "-doc-source-url",
                    "https://bitbucket.org/inkytonik/kiama/src/master€{FILE_PATH}.scala"
            ),
        TestScalaUnidoc/unidoc/scalacOptions := (ScalaUnidoc/unidoc/scalacOptions).value,
        ScalaUnidoc/unidoc/unidocProjectFilter := inAnyProject -- inProjects(extrasProject),
        TestScalaUnidoc/unidoc/unidocProjectFilter := (ScalaUnidoc/unidoc/unidocProjectFilter).value
    ).dependsOn(
        base % "compile-internal; test-internal"
    )

lazy val extras =
    setupSubProject(
        project in file("extras"),
        "kiama-extras"
    ).settings(
        libraryDependencies ++=
            Seq(
                // Command-line handling:
                "org.rogach" %%% "scallop" % "3.4.0",
                // Language server protocol:
                "org.eclipse.lsp4j" % "org.eclipse.lsp4j" % "0.8.1",
                "com.google.code.gson" % "gson" % "2.7",
                // REPLs:
                "jline" % "jline" % "2.14.6"
            ),
        javaOptions ++= Seq("-Xss8M"),
        fork := false,
        run/connectInput := true,
        run/outputStrategy := Some(StdoutOutput),
        Test/console/initialCommands :=
            (Test/console/initialCommands).value + """
                import org.bitbucket.inkytonik.kiama._
                import example.json.PrettyPrinter._
                import example.json.JSONTree._
            """.stripMargin,
        Compile/doc/scalacOptions ++=
            Seq(
                if (scalaVersion.value.startsWith("2.10"))
                    "-Ymacro-no-expand"
                else
                    "-Ymacro-expand:none",
                "-doc-source-url",
                    "https://bitbucket.org/inkytonik/kiama/src/master€{FILE_PATH}.scala"
            ),
        Test/doc/scalacOptions := (Compile/doc/scalacOptions).value
    ).settings(
       inConfig(Test)(baseAssemblySettings)
    ).settings(
        // Test/assembly/test := {},
        Test/assembly/assemblyJarName := s"${name.value}-assembly-${version.value}-tests.jar"
    ).dependsOn(
        base % "compile-internal; test-internal",
        core % "compile; test->test"
    )

lazy val root =
    setupProject(
        project in file("."),
        "root"
    ).settings(
        noPublishSettings : _*
    ).aggregate(
        core, extras
    )
