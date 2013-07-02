package tjmx

import fr.janalyse.jmx._
import scopt._
import scala.util.control.Exception._

case class Params(
  queries: List[String] = List("java.lang:type=*"),
  vmRegex: String = "*",
  intervalSecs: Long = 15
)


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

  def processConnections(conns: Map[Int, JMX]): Map[Int, JMX] = {
    conns.filter{ pidConnPair =>
      val resultOpt = catching(classOf[Exception]) opt {
        println(pidConnPair._1)
        pidConnPair._2.mbeans.foreach{ mbean =>
          println(mbean)
        }
      }

      resultOpt match{
        case Some(_) => true
        case _ => false
      }
    }
  }

  def replenishConnections(conns: Map[Int, JMX], vms: Map[Int, VM]): Map[Int, JMX] = {
    conns ++ vms.filterKeys( !conns.contains(_) ).mapValues( vm => JMX(JMXOptions(url = vm.serviceUrl)) )
  }

  parser.parse(args, Params()) map { params =>
    Stream.iterate(replenishConnections(Map(), VMUtils.listLocalVms)){ conns =>
      val validConns = processConnections(conns)
      replenishConnections(validConns, VMUtils.listLocalVms)
    }.force
  }

}
