import akka.stream._
import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import akka.stream.stage._
import java.nio.file._
import WatchEvent.{Kind => WEKind}
import StandardWatchEventKinds._
import scala.collection.JavaConverters._

case class FSWatchSource(
  dir: Path
) extends GraphStage[SourceShape[List[(Path,WEKind[Path])]]] {

  val out: Outlet[List[(Path,WEKind[Path])]] = Outlet("PathsWatcherSource")

  override val shape: SourceShape[List[(Path,WEKind[Path])]] = SourceShape(out)

  override def createLogic(inherited: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) {

      val watcher = FileSystems.getDefault.newWatchService
      addSubdir(dir)

      def addSubdir(d: Path) {
        d.register(watcher,
          ENTRY_CREATE,
          ENTRY_DELETE,
          ENTRY_MODIFY
        )

        val children = d.toFile.listFiles.filter(_.isDirectory).foreach {
          x => addSubdir(x.toPath)
        }
      }

      setHandler(out, new OutHandler {
        override def onPull() {
          val key = watcher.take()
          val events: List[(Path,WEKind[Path])] = key.pollEvents.asScala.toList.map { ev =>
            val event = ev.asInstanceOf[WatchEvent[Path]]
            val parentDir: Path = key.watchable.asInstanceOf[Path]
            val fullPath = parentDir.resolve(event.context)

            if (Files.isDirectory(fullPath) && event.kind == ENTRY_CREATE) {
              addSubdir(fullPath)
            }

            (fullPath, event.kind)
          }
          key.reset
          push(out, events)
        }
      })
    }
}

object SampleApp extends App {

  implicit val system = ActorSystem("QuickStart")
  implicit val materializer = ActorMaterializer()

  val tempDir = Files.createTempDirectory(Paths get "/tmp", "fswatcher")
  println(s"I'm watching $tempDir for changes")

  val sourceGraph = Source.fromGraph(FSWatchSource(tempDir)).mapConcat(identity)

  sourceGraph.runForeach {println} (materializer)
}
