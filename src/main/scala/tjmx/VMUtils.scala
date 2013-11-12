package tjmx

import com.sun.tools.attach.VirtualMachine
import com.sun.tools.attach.VirtualMachineDescriptor
import java.io.File
import javax.management.remote.JMXServiceURL
import sun.jvmstat.monitor.HostIdentifier
import sun.jvmstat.monitor.MonitoredVmUtil
import sun.jvmstat.monitor.VmIdentifier
import sun.management.ConnectorAddressLink

import fr.janalyse.jmx._

import sun.jvmstat.monitor.MonitoredHost
import scala.collection.JavaConversions._

import scala.util.control.Exception._

case class VM(pid: Int, name: String, attachable: Boolean)

object VMUtils{
   private final var LOCAL_CONNECTOR_ADDRESS = "com.sun.management.jmxremote.localConnectorAddress";

  // Yet another copy of what jconsole does
  def listLocalVms : Map[Int, VM] = {
    val hotspotVms = getMonitoredVMs
    val other = getAttachableVMs(hotspotVms)
    hotspotVms ++ other
  }

  private def getMonitoredVMs: Map[Int,VM] = {
    val host = MonitoredHost.getMonitoredHost(new HostIdentifier(null.asInstanceOf[String]))
    val vmPids = host.activeVms()
    vmPids.map{ pid =>
      try {        
        val vm = host.getMonitoredVm(new VmIdentifier(pid.toString))
        try{
          val result = VM(pid, MonitoredVmUtil.mainClass(vm, false), MonitoredVmUtil.isAttachable(vm))
          Some(result.pid, result)
        } finally {
          vm.detach
        }
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

  // def attemptConnect(pids: Set[Int]) : Map[Int, Option[VMConnection]] = {

  // }

  private def getAttachableVMs(knownVms: Map[Int, VM]): Map[Int, VM] = {
    val filteredVMDList = VirtualMachine.list().filter( vmd => !knownVms.contains( vmd.id().toInt ) )
    filteredVMDList.
      collect{ vmd: VirtualMachineDescriptor =>
        catching(classOf[Exception]) opt {
          val pid = vmd.id().toInt
          (pid, VM(pid, vmd.displayName(), true))
        } match {
          case Some(pair) => pair
        }
      }.toMap

  }

  private def getJavaHome(vm: VirtualMachine) = {
    Option(vm.getSystemProperties().getProperty("java.home")).getOrElse{
      throw new RuntimeException("Unable to get the java home directory for PID: " + vm.id)
    }
  }

  private def loadManagementLibs(vm: VirtualMachine) = {
    val home = getJavaHome(vm)

    val managementLibCandidates = List(
      new File(home + File.separator + "lib" + File.separator +"management-agent.jar"),
      new File(home + File.separator +"management-agent.jar") )

    val mgmtAgentFile = managementLibCandidates.find( _.exists ).getOrElse{
      throw new RuntimeException("Couldn't find management agent jar for pid: " + vm.id)
    }

    vm.loadAgent(mgmtAgentFile.getCanonicalPath(), "com.sun.management.jmxremote")
  }

  private def getConnectorAddress(vm : VirtualMachine) = {
    Option(vm.getAgentProperties().get(LOCAL_CONNECTOR_ADDRESS).asInstanceOf[String]).getOrElse{
      throw new RuntimeException("Unable to get service connector url for pid: " + vm.id)
    }
  }


  def attemptConnection(vm: VM) : VMConnection = {
    val connection = catching(classOf[Exception]) either {
      val url =  getUrl(vm).getOrElse("")
      JMX(JMXOptions(url = new JMXServiceURL(url)))
    }
    VMConnection(connection.left.map { ex => System.currentTimeMillis   }, vm)
  }

  private def getUrl(vm: VM) = {
    val pid = vm.pid
    Option(ConnectorAddressLink.importFrom(vm.pid)).orElse{
      if(vm.attachable) {
        Option(VirtualMachine.attach(vm.pid.toString)) map { vm =>
          try {
            loadManagementLibs(vm)
            getConnectorAddress(vm)
          } finally {
            vm.detach
          }
        }
      }
      else {
        None
      }

    }
  }
}

