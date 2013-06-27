package tjmx

import fr.janalyse.jmx._
import scopt._


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

  parser.parse(args, Params()) map { params =>
    val vms = VMUtils.listLocalVms
    Stream.continually{
      vms.values.foreach{ vm =>
        val jmx = JMX(JMXOptions(url = vm.serviceUrl))
          println(vm.pid)
        jmx.mbeans.foreach{ mbean =>
          println(mbean)
        }
    }
    }.force
  }

}
