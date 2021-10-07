lazy val commonSettings = Seq(
  organization := "com.infuse-ev",
  scalaVersion := "2.12.11",
  crossScalaVersions := Seq("2.12.11"),
  scalacOptions ++= Seq("-Xlint:-nullary-unit"),
  javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint")
)

val publishSettings = Seq(
  publishTo := sonatypePublishToBundle.value,
  sonatypeCredentialHost := "s01.oss.sonatype.org",

  scmInfo := Some(ScmInfo(
    url("https://github.com/IHomer/docile-charge-point"),
    "scm:git@github.com:IHomer/docile-charge-point.git"
  )),

  description := "Scriptable OCPP charge point simulator",

  licenses := Seq("GPLv3" -> new URL("https://www.gnu.org/licenses/gpl-3.0.en.html")),

  homepage := Some(url("https://github.com/IHomer/docile-charge-point")),

  developers := List(
    Developer(id="tux_rocker", name="Reinier Lamers", email="reinier.lamers@ihomer.nl", url=url("http://reinier.de/"))
  )
)

lazy val commandLine = (project in file("cmd"))
  .dependsOn(core)
  .dependsOn(loader)
  .settings(
    commonSettings,
    publishSettings,
    name := "docile-charge-point-command-line",
    libraryDependencies ++= commandLineDeps,
    mainClass := Some("chargepoint.docile.Main"),
    assemblyJarName in assembly := "docile.jar",
    assemblyMergeStrategy in assembly := {
        case "reflect.properties" =>
            MergeStrategy.concat
	case x =>
	    val oldStrategy = (assemblyMergeStrategy in assembly).value
	    oldStrategy(x)
    },
    connectInput in run := true
  )

lazy val core = (project in file("core"))
  .settings(
    commonSettings,
    publishSettings,
    name := "docile-charge-point",
    libraryDependencies ++= coreDeps
  )

lazy val loader = (project in file("loader"))
  .dependsOn(core)
  .settings(
    commonSettings,
    publishSettings,
    libraryDependencies ++= loaderDeps(scalaVersion.value),
    name := "docile-charge-point-loader"
  )

lazy val coreDeps = Seq(
  "com.infuse-ev"               %% "ocpp-j-api"       % "9.2.4",
  "com.typesafe.scala-logging"  %% "scala-logging"    % "3.9.0",

  "org.scalatest"               %% "scalatest"        % "3.2.2"    % "test",
  "org.scalamock"               %% "scalamock"        % "5.1.0"    % "test"
)

def loaderDeps(scalaVersion: String) = Seq(
  "org.scala-lang"               % "scala-compiler"   % scalaVersion,
)

lazy val commandLineDeps = Seq(
  "com.lihaoyi"                  % "ammonite"         % "2.1.4"    cross CrossVersion.full,
  "org.rogach"                  %% "scallop"          % "3.1.3",
  "ch.qos.logback"               % "logback-classic"  % "1.2.3"
)

commonSettings
publishSettings

name := "docile-charge-point-root"

publish / skip := true