package akka.testkit

import java.io.File
import com.typesafe.config._

import scala.util._

import akka.actor._
import akka.persistence._
import org.scalatest._

import org.apache.commons.io.FileUtils

abstract class PersistenceSpec(system: ActorSystem) extends TestKit(system)
  with ImplicitSender
  with WordSpecLike
  with BeforeAndAfterAll
  with PersistenceCleanup {

  def this(name: String, config: Config) = this(ActorSystem(name, config))
  override protected def beforeAll() = deleteStorageLocations()

  override protected def afterAll() = {
    deleteStorageLocations()
    TestKit.shutdownActorSystem(system)
  }
}

trait PersistenceCleanup {
  def system: ActorSystem

  val storageLocations = List(
    "akka.persistence.journal.leveldb.dir",
    "akka.persistence.journal.leveldb-shared.store.dir",
    "akka.persistence.snapshot-store.local.dir").map(s ⇒ new File(system.settings.config.getString(s)))

  def deleteStorageLocations(): Unit = {
    storageLocations.foreach(dir ⇒ Try(FileUtils.deleteDirectory(dir)))
  }
}

// taken from akka persistence
object PersistenceSpec {
  def config(plugin: String, test: String, native: String = "off", serialization: String = "on", extraConfig: Option[String] = None) =
    extraConfig.map(ConfigFactory.parseString(_)).getOrElse(ConfigFactory.empty()).withFallback(
      ConfigFactory.parseString(
        s"""
      akka.actor.serialize-creators = ${serialization}
      akka.actor.serialize-messages = ${serialization}
      akka.persistence.publish-confirmations = on
      akka.persistence.publish-plugin-commands = on
      akka.persistence.journal.plugin = "akka.persistence.journal.${plugin}"
      akka.persistence.journal.leveldb.native = ${native}
      akka.persistence.journal.leveldb.dir = "target/journal-${test}"
      akka.persistence.snapshot-store.local.dir = "target/snapshots-${test}/"
      akka.test.single-expect-default = 10s
    """))
}
