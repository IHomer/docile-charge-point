lazy val commonSettings = Seq(
  organization := "com.infuse-ev",
  scalaVersion := "2.12.11",
  crossScalaVersions := Seq("2.12.11"),
  scalacOptions ++= Seq("-Xlint:-nullary-unit"),
  javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint")
)

lazy val commandLine = (project in file("cmd"))
  .dependsOn(core)
  .dependsOn(loader)
  .enablePlugins(OssLibPlugin)
  .settings(
    commonSettings,
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
  .enablePlugins(OssLibPlugin)
  .settings(
    commonSettings,
    name := "docile-charge-point",
    libraryDependencies ++= coreDeps
  )

lazy val loader = (project in file("loader"))
  .dependsOn(core)
  .enablePlugins(OssLibPlugin)
  .settings(
    commonSettings,
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

enablePlugins(OssLibPlugin)

commonSettings

name := "docile-charge-point-root"
