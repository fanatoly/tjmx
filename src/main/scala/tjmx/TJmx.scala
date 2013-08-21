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
  blacklistPeriod: Long = 6 * 3600000,
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
    val stats = TJmxStats()
    val printer = new MBeanPrinter(params.queries.map{ "(" + _ + ")" }.mkString("|").r, stats)

    def processConnections(conns: Map[Int, VMConnection]): Map[Int, VMConnection] = {
      conns.map{ 
        case conn@(pid, VMConnection(Right(jmx), vm)) =>
          catching(classOf[Throwable]) either {
            printer.output(jmx, vm)
          } match {
            case Left(ex) => {
              jmx.close
              stats.readErrorCount = stats.readErrorCount + 1
              if(params.debug) ex.printStackTrace();
              (pid, VMConnection(Left(params.blacklistPeriod), vm))
            }
            case _ => conn
          }
        case conn => conn
      }
    }

    def replenishConnections(conns: Map[Int, VMConnection], vms: Map[Int, VM]):
        Map[Int, VMConnection] = {

      //This list will contain connections that are either on timeout, resulting from a 
      //failed connection attempt, or with valid connections. This is the set of connections
      //that DO NOT require a retry.
      val retainedConns = conns.filter{
        case (pid, VMConnection(Left(failureTime), _)) =>
          failureTime >= System.currentTimeMillis - params.blacklistPeriod
        case _ => true
      }

      val newConns = vms.filterKeys( !retainedConns.contains(_) ).
        filter{ case (pid, vm) => params.vmRegex.findFirstIn(vm.name).isDefined }.
        map{ vm =>
          val newConn = VMUtils.attemptConnection(vm._2)
          if(newConn.jmx.isLeft) stats.connectErrorCount = stats.connectErrorCount + 1
          (vm._1 , newConn)
        }

      if(params.debug) println(s"Replenish results: retained=${retainedConns.size} new=${newConns.size}")
      retainedConns ++ newConns
    }


    def updateStats(conns: Map[Int, VMConnection], sleepTime: Long) = {
      stats.lastSleepTime = sleepTime
      stats.blacklistCount = conns.values.count{
        case VMConnection(Left(_), _) => true
        case _ => false
      }
      stats.connectionCount = conns.size
    }

    Stream.iterate(Map[Int, VMConnection]()){ conns =>
      val timeIterStart = System.currentTimeMillis
      val connectionSet = replenishConnections(conns, VMUtils.listLocalVms)
      val validConns = processConnections(connectionSet)

      val sleepVal = Math.max(0, params.intervalSecs * 1000 - (System.currentTimeMillis - timeIterStart))
      updateStats(validConns, sleepVal)
      if(params.debug) 
        println(s"Next wake up in ${sleepVal}, Succesfully pulled data from: ${validConns.size}")

      Thread.sleep(sleepVal)
      validConns
    }.force
  }

}
