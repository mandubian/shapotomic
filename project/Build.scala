import sbt._
import Keys._

object BuildSettings {
  val buildName              = "shapotomic"
  val buildOrganization      = "org.mandubian"

  val buildScalaVersion      = "2.10.0"

  val buildSettings = Defaults.defaultSettings ++ Seq (
    scalaVersion    := buildScalaVersion,
    organization    := buildOrganization
  )
}

object ApplicationBuild extends Build {

  val mandubianRepo = Seq(
    "Mandubian repository snapshots" at "https://github.com/mandubian/mandubian-mvn/raw/master/snapshots/",
    "Mandubian repository releases" at "https://github.com/mandubian/mandubian-mvn/raw/master/releases/"
  )

  val sonatypeRepo = Seq(
    "Sonatype OSS Releases" at "http://oss.sonatype.org/content/repositories/releases/",
    "Sonatype OSS Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/"    
  )

  val datomicRepo = Seq(
    "datomisca-repo snapshots" at "https://github.com/pellucidanalytics/datomisca-repo/raw/master/snapshots",
    "datomisca-repo releases"  at "https://github.com/pellucidanalytics/datomisca-repo/raw/master/releases",
    // to get Datomic free (for pro, you must put in your own repo or local)
    "clojars" at "https://clojars.org/repo"
  )

  lazy val shapotomic = Project(
    BuildSettings.buildName, file("."),
    settings = BuildSettings.buildSettings ++ Seq(
      resolvers ++= mandubianRepo ++ sonatypeRepo ++ datomicRepo,
      libraryDependencies ++= Seq(
        "com.chuusai"       %% "shapeless"    % "1.2.4",
        "pellucidanalytics" %% "datomisca"    % "0.3-SNAPSHOT",
        "com.datomic"       %  "datomic-free" % "0.8.3814",
        "org.specs2"        %% "specs2"       % "1.13" % "test",
        "junit"             %  "junit"        % "4.8" % "test"
      ),
      publishMavenStyle := true,
      publishTo <<= version { (version: String) =>
        val localPublishRepo = "../mandubian-mvn/"
        if(version.trim.endsWith("SNAPSHOT"))
          Some(Resolver.file("snapshots", new File(localPublishRepo + "/snapshots")))
        else Some(Resolver.file("releases", new File(localPublishRepo + "/releases")))
      },
      scalacOptions ++= Seq(
        //"-Xlog-implicits"
        //"-deprecation",
        //"-feature"
      )
    )
  )
}
