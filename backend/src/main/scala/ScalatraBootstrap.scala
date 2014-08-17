import edu.nyu.dlts.drpbx._
import org.scalatra._
import javax.servlet.ServletContext
import _root_.akka.actor.ActorSystem


class ScalatraBootstrap extends LifeCycle {
  
  val system = ActorSystem()

  override def init(context: ServletContext) {
    context.mount(new Backend(system), "/*")
  }

  override def destroy(context:ServletContext) {
    system.shutdown()
  }
}
