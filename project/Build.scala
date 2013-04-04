import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "Play2JavaRabbitMQ"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    "com.rabbitmq" % "amqp-client" % "2.8.1",
    javaCore,
    javaJdbc,
    javaEbean
  )

  val main = play.Project(appName, appVersion, appDependencies).settings(
    // Add your own project settings here      
  )

}
