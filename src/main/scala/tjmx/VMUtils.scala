package tjmx

import com.sun.tools.attach.VirtualMachine
import java.io.File
import javax.management.remote.JMXServiceURL
import sun.jvmstat.monitor.HostIdentifier
import sun.jvmstat.monitor.MonitoredVmUtil
import sun.jvmstat.monitor.VmIdentifier
import sun.management.ConnectorAddressLink

import sun.management.ConnectorAddressLink
import sun.jvmstat.monitor.MonitoredHost
import scala.collection.JavaConversions._

import scala.util.control.Exception._

case class VM(pid: Int, name: String, serviceUrl: JMXServiceURL)

object VMUtils{
   private final var LOCAL_CONNECTOR_ADDRESS = "com.sun.management.jmxremote.localConnectorAddress";

  // Yet another copy of what jconsole does
  def listLocalVms : Map[Int, VM] = getMonitoredVMs ++ getAttachableVMs

  private def getMonitoredVMs: Map[Int,VM] = {
    val host = MonitoredHost.getMonitoredHost(new HostIdentifier(null.asInstanceOf[String]))
    val vmPids = host.activeVms()
    vmPids.map{ pid =>
      try {
        val vm = host.getMonitoredVm(new VmIdentifier(pid.toString))
        val result = VM(pid, MonitoredVmUtil.commandLine(vm), new JMXServiceURL(getUrl(pid)))
        //TODO: this leaks connections
        vm.detach
        Some(result.pid, result)
      } catch {
        case ex: Exception => {
          ex.printStackTrace()
          None
        }
      }
    }.collect{
        case Some(pair) => pair
    }.toMap
  }

  private def getAttachableVMs: Map[Int, VM] = {
    val resultMaybe = catching(classOf[Exception]) opt {
      VirtualMachine.list().map{ vmd =>
        val pid = vmd.id().toInt
        (pid, VM(pid, vmd.displayName(), new JMXServiceURL(getUrl(pid))))
      }.toMap
    }
    resultMaybe.getOrElse( Map() )
  }

  private def getUrl(pid: Int) = {
    Option(ConnectorAddressLink.importFrom(pid)).getOrElse{
      val resultMaybe = Option(VirtualMachine.attach(pid.toString)) map { vm =>
        val home = Option(vm.getSystemProperties().getProperty("java.home")).getOrElse{
          throw new RuntimeException("Unable to get the java home directory for PID: " + pid)
        }

        val managementLibCandidates = List(
          new File(home + File.separator + "lib" + File.separator +"management-agent.jar"),
          new File(home + File.separator +"management-agent.jar") )

        val mgmtAgentFile = managementLibCandidates.find( _.exists ).getOrElse{
          throw new RuntimeException("Couldn't find management agent jar for pid: " + pid)
        }

        vm.loadAgent(mgmtAgentFile.getCanonicalPath(), "com.sun.management.jmxremote")
        Option(vm.getAgentProperties().get(LOCAL_CONNECTOR_ADDRESS).asInstanceOf[String]).getOrElse{
          throw new RuntimeException("Unable to get service connector url for pid: " + pid)
        }
      }

      resultMaybe.get
    }
  }
}
