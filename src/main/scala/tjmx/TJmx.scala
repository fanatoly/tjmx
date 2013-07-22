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
  blacklistPeriod: Long = 3600000,
  debug: Boolean = false
)

case class VMConnection(jmx: Either[Long, JMX], vm: VM);

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
      conns.filter{ 
        case (_, VMConnection(Right(jmx), vm)) =>
          catching(classOf[Exception]) either {
            printer.output(jmx, vm)
          } match {
            case Left(ex) => {
              if(params.debug) ex.printStackTrace();
              false
            }
            case _ => true
          }
        case _ => true
      }
    }

    def replenishConnections(conns: Map[Int, VMConnection], vms: Map[Int, VM]): Map[Int, VMConnection] = {
      val filteredConns = conns.filter{
        case (pid, VMConnection(Left(failureTime), _)) =>
          failureTime >= System.currentTimeMillis - params.blacklistPeriod
        case _ => true
      }

      filteredConns ++ vms.filterKeys( !filteredConns.contains(_) ).
        filter{
          case (pid, vm) => params.vmRegex.findFirstIn(vm.name).isDefined
        }.
        map{ vm => (vm._1 ,VMUtils.attemptConnection(vm._2)) }
    }


    Stream.iterate(Map[Int, VMConnection]()){ conns =>
      val timeIterStart = System.currentTimeMillis
      val connectionSet = replenishConnections(conns, VMUtils.listLocalVms)
      val validConns = processConnections(connectionSet)

      val sleepVal = Math.max(0, params.intervalSecs * 1000 - (System.currentTimeMillis - timeIterStart))
      if(params.debug) 
        println(s"Next wake up in ${sleepVal}, Succesfully pulled data from: ${validConns.size}")

      Thread.sleep(sleepVal)
      validConns
    }.force
  }

}
