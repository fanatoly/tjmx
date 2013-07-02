package tjmx

import fr.janalyse.jmx._
import scala.util.matching.Regex

class MBeanPrinter(queries: List[Regex]){
  def output(conn: VMConnection){
    conn.jmx.mbeans.foreach{ mbean =>
      mbean.attributes.foreach{
        case dbl: RichNumberAttribute => mbean.getString(dbl).map{ printVal(conn.vm.name, mbean, dbl.name, _) }
        case composite: RichCompositeDataAttribute => mbean.getNumberComposite(composite).map { numMap =>
          numMap.foreach{ kv =>
            printVal(conn.vm.name, mbean, composite.name + "." + kv._1, kv._2)
          }
        }
        case _ =>
      }
    }
  }

  private def printVal[T](procName: String, mbean: RichMBean, name: String, v: T){
    println(s"${mbean.name}, ${name} ${v}, procName=${procName}")
  }

}
