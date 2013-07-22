package tjmx

import fr.janalyse.jmx._
import scala.util.matching.Regex
import scala.util.matching.Regex._
import scopt._
import scala.util.control.Exception._

case class Params(
  queries: List[String] = List(".*java.lang.*"),
  vmRegex: Regex =".*".r,
  intervalSecs: Long = 15,
  debug: Boolean = false
)

case class VMConnection(jmx: JMX, vm: VM);

object TJmx extends App{
  val parser = new OptionParser[Params]("tjmx"){
    help("help")
    opt[String]('f', "metric-filters") valueName("<list of filter regexps>") action { (queries, params)=>
      params.copy(queries = queries.split(",").toList)
    }
    opt[String]('x', "exclude-metrics") valueName("<list of regexps to exclude>") action { (queries, params)=>
      params.copy(queries = queries.split(",").toList)
    }
    opt[String]('r', "vm-regex") valueName("<VM name regex>") action { (regex, params)=>
      params.copy(vmRegex = regex.r)
    }
    opt[Boolean]('d', "debug") valueName("debug me") action { (flag, params)=>
      params.copy(debug = flag)
    }
    opt[Long]('i', "interval") action{ (i, params) =>
      params.copy(intervalSecs = i)
    }
  }

  parser.parse(args, Params()) map { params =>
    val printer = new MBeanPrinter(params.queries.map{ "(" + _ + ")" }.mkString("|").r)

    def processConnections(conns: Map[Int, VMConnection]): Map[Int, VMConnection] = {
      conns.filter{ pidConnPair =>
        catching(classOf[Exception]) either {
          printer.output(pidConnPair._2)
        } match {          
          case Left(ex) => { 
            if(params.debug) ex.printStackTrace();
            false
          }
          case _ => true
        }
      }
    }

    def replenishConnections(conns: Map[Int, VMConnection], vms: Map[Int, VM]): Map[Int, VMConnection] = {
      conns ++ vms.filterKeys( !conns.contains(_) ).
        filter{
          case (pid, vm) => params.vmRegex.findFirstIn(vm.name).isDefined
        }.
        collect{ vm: (Int, VM) =>
          catching(classOf[Exception]) opt {
            (vm._1, VMConnection(JMX(JMXOptions(url = vm._2.serviceUrl)), vm._2))
          } match {
            case Some(entry) => entry
          }
        }
    }


    Stream.iterate(replenishConnections(Map(), VMUtils.listLocalVms)){ conns =>
      val timeIterStart = System.currentTimeMillis
      val validConns = processConnections(conns)
      val result = replenishConnections(validConns, VMUtils.listLocalVms)
      val sleepVal = Math.max(0, params.intervalSecs * 1000 - (System.currentTimeMillis - timeIterStart))
      if(params.debug) println(s"Next wake up in ${sleepVal}, Number of VMs that will be read: ${result.size}")
      Thread.sleep(sleepVal)
      result
    }.force
  }

}
