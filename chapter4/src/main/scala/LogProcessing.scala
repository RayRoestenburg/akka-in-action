package ch03

import akka.actor._
import java.io.File
import akka.actor.SupervisorStrategy.{ Stop, Resume, Restart }
import akka.actor.OneForOneStrategy
import scala.concurrent.duration._
import language.postfixOps

package dbstrategy1 {
  //<start id="ch03-build-hierarchy"/>
  object LogProcessingApp extends App {
    val sources = Vector("file:///source1/", "file:///source2/")
    val system = ActorSystem("logprocessing")
    // create the props and dependencies
    val con = new DbCon("http://mydatabase")
    val writerProps = Props(new DbWriter(con))
    val dbSuperProps = Props(new DbSupervisor(writerProps))
    val logProcSuperProps = Props(
      new LogProcSupervisor(dbSuperProps))
    val topLevelProps = Props(new FileWatchingSupervisor(
      sources,
      logProcSuperProps))
    system.actorOf(topLevelProps) //<co id="ch03-top-level-actor"/>
  }
  //<end id="ch03-build-hierarchy"/>

  //<start id="ch03-filewatcher-supervisor"/>
  class FileWatchingSupervisor(sources: Vector[String],
                               logProcSuperProps: Props)
    extends Actor {

    var fileWatchers: Vector[ActorRef] = sources.map { source =>
      val logProcSupervisor = context.actorOf(logProcSuperProps)
      val fileWatcher = context.actorOf(Props(
        new FileWatcher(source, logProcSupervisor)))
      context.watch(fileWatcher) //<co id="ch03-filewatcher-supervisor-watch"/>
      fileWatcher
    }

    override def supervisorStrategy = AllForOneStrategy() {
      case _: DiskError => Stop //<co id="ch03-filewatcher-supervisor-stop"/>
    }

    def receive = {
      case Terminated(fileWatcher) => //<co id="ch03-filewatcher-terminated"/>
        fileWatchers = fileWatchers.filterNot(w => w == fileWatcher)
        if (fileWatchers.isEmpty) self ! PoisonPill //<co id="ch03-filewatcher-supervisor-suicide"/>
    }
  }
  //<end id="ch03-filewatcher-supervisor"/>

  //<start id="ch03-filewatcher"/>
  class FileWatcher(sourceUri: String,
                    logProcSupervisor: ActorRef)
    extends Actor with FileWatchingAbilities {
    register(sourceUri) //<co id="ch03-filewatcher-register"/>

    import FileWatcherProtocol._
    import LogProcessingProtocol._

    def receive = {
      case NewFile(file, _) => //<co id="ch03-filewatcher-receive-newfile"/>
        logProcSupervisor ! LogFile(file) //<co id="ch03-filewatcher-forward"/>
      case SourceAbandoned(uri) if uri == sourceUri =>
        self ! PoisonPill //<co id="ch03-filewatcher-suicide"/>
    }
  }
  //<end id="ch03-filewatcher"/>

  //<start id="ch03-logprocessor-supervisor"/>
  class LogProcSupervisor(dbSupervisorProps: Props)
    extends Actor {
    override def supervisorStrategy = OneForOneStrategy() {
      case _: CorruptedFileException => Resume //<co id="ch03-logprocessor-resume"/>
    }
    val dbSupervisor = context.actorOf(dbSupervisorProps) //<co id="ch03-dbsupervisor-create"/>
    val logProcProps = Props(new LogProcessor(dbSupervisor))
    val logProcessor = context.actorOf(logProcProps) //<co id="ch03-logprocessor-create"/>

    def receive = {
      case m => logProcessor forward (m) //<co id="ch03-logprocessor-forward"/>
    }
  }
  //<end id="ch03-logprocessor-supervisor"/>

  //<start id="ch03-logprocessor"/>
  class LogProcessor(dbSupervisor: ActorRef)
    extends Actor with LogParsing {
    import LogProcessingProtocol._
    def receive = {
      case LogFile(file) =>
        val lines = parse(file) //<co id="ch03-logprocessor-dangerous-operation"/>
        lines.foreach(dbSupervisor ! _) //<co id="ch03-logprocessor-send-writer"/>
    }
  }
  //<end id="ch03-logprocessor"/>
  //<start id="ch03-impatient-dbsupervisor"/>
  class DbImpatientSupervisor(writerProps: Props) extends Actor {
    override def supervisorStrategy = OneForOneStrategy(
      maxNrOfRetries = 5,
      withinTimeRange = 60 seconds) { //<co id="ch03-impatient"/>
        case _: DbBrokenConnectionException => Restart
      }
    val writer = context.actorOf(writerProps)
    def receive = {
      case m => writer forward (m)
    }
  }
  //<end id="ch03-impatient-dbsupervisor"/>

  //<start id="ch03-dbsupervisor"/>
  class DbSupervisor(writerProps: Props) extends Actor {
    override def supervisorStrategy = OneForOneStrategy() {
      case _: DbBrokenConnectionException => Restart //<co id="ch03-dbwriter-restart"/>
    }
    val writer = context.actorOf(writerProps) //<co id="ch03-dbwriter-create"/>
    def receive = {
      case m => writer forward (m) //<co id="ch03-dbwriter-forward"/>
    }
  }
  //<end id="ch03-dbsupervisor"/>

  //<start id="ch03-dbwriter"/>
  class DbWriter(connection: DbCon) extends Actor {
    import LogProcessingProtocol._
    def receive = {
      case Line(time, message, messageType) =>
        connection.write(Map('time -> time,
          'message -> message,
          'messageType -> messageType)) //<co id="ch03-dbwriter-dangerous-operation"/>
    }
  }
  //<end id="ch03-dbwriter"/>
}
class DbCon(url: String) {
  /**
   * Writes a map to a database.
   * @param map the map to write to the database.
   * @throws DbBrokenConnectionException when the connection is broken. It might be back later
   * @throws DbNodeDownException when the database Node has been removed from the database cluster. It will never work again.
   */
  def write(map: Map[Symbol, Any]) {
    //
  }
}
//<start id="ch03-strategies-exceptions"/>
@SerialVersionUID(1L)
class DiskError(msg: String)
  extends Error(msg) with Serializable //<co id="ch03-diskerror"/>

@SerialVersionUID(1L)
class CorruptedFileException(msg: String, val file: File)
  extends Exception(msg) with Serializable //<co id="ch03-corruptedfileexception"/>

@SerialVersionUID(1L)
class DbBrokenConnectionException(msg: String)
  extends Exception(msg) with Serializable //<co id="ch03-dbbrokenexception"/>
//<end id="ch03-strategies-exceptions"/>

trait LogParsing {
  import LogProcessingProtocol._
  // Parses log files. creates line objects from the lines in the log file.
  // If the file is corrupt a CorruptedFileException is thrown
  def parse(file: File): Seq[Line] = {
    Nil
  }
}
object FileWatcherProtocol {
  case class NewFile(file: File, timeAdded: Long)
  case class SourceAbandoned(uri: String)
}
trait FileWatchingAbilities {
  def register(uri: String) {

  }
}

//<start id="ch03-strategies-logprocessing"/>
object LogProcessingProtocol {
  // represents a new log file
  case class LogFile(file: File) //<co id="ch03-logfile"/>
  // A line in the log file parsed by the LogProcessor Actor
  case class Line(time: Long, message: String, messageType: String) //<co id="ch03-line"/>
}
//<end id="ch03-strategies-logprocessing"/>
