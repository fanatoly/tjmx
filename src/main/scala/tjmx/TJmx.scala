package tjmx

import fr.janalyse.jmx._
import java.util.regex.Pattern
import scala.util.matching.Regex._
import scopt._
import scala.util.control.Exception._

case class Params(
  queries: List[String] = List("java.lang:type=*"),
  vmRegex: String = "*",
  intervalSecs: Long = 15
)

case class VMConnection(jmx: JMX, vm: VM);

object TJmx extends App{
  val parser = new OptionParser[Params]("TJmx"){
    head("TJmx")
    help("help")
    opt[String]('q', "queries") valueName("<list of queries>") action { (queries, params)=>
      params.copy(queries = queries.split(",").toList)
    }
    opt[String]('r', "vm-regex") valueName("<regex>") action { (regex, params)=>
      params.copy(vmRegex = regex)
    }

    opt[Long]('i', "interval") action{ (i, params) =>
      params.copy(intervalSecs = i)
    }
  }

  parser.parse(args, Params()) map { params =>
    val printer = new MBeanPrinter(params.queries.map{ _.r })

    def processConnections(conns: Map[Int, VMConnection]): Map[Int, VMConnection] = {
      conns.filter{ pidConnPair =>
        val resultOpt = catching(classOf[Exception]) opt {
          printer.output(pidConnPair._2)
        }
        resultOpt.isDefined
      }
    }

    def replenishConnections(conns: Map[Int, VMConnection], vms: Map[Int, VM]): Map[Int, VMConnection] = {
      conns ++ vms.filterKeys( !conns.contains(_) ).mapValues{ vm =>
        VMConnection(JMX(JMXOptions(url = vm.serviceUrl)), vm)
      }
    }


    Stream.iterate(replenishConnections(Map(), VMUtils.listLocalVms)){ conns =>
      val validConns = processConnections(conns)
      val result = replenishConnections(validConns, VMUtils.listLocalVms)
      Thread.sleep(params.intervalSecs * 1000)
      result
    }.force
  }

}
