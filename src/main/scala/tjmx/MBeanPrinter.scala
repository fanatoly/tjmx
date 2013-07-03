package tjmx

import fr.janalyse.jmx._
import scala.util.matching.Regex
import scala.collection.JavaConversions._


class MBeanPrinter(queries: List[Regex]){

  def output(conn: VMConnection){
    val procName = getProcessName(conn)
    conn.jmx.mbeans.foreach{ mbean =>
      mbean.attributes.foreach{
        case dbl: RichNumberAttribute => mbean.getString(dbl).map{ printVal(procName, mbean, dbl.name, _) }
        case composite: RichCompositeDataAttribute => mbean.getNumberComposite(composite).map { numMap =>
          numMap.foreach{ kv =>
            printVal(procName, mbean, composite.name + "." + kv._1, kv._2)
          }
        }
        case _ =>
      }
    }
  }

  private def printVal[T](procName: String, mbean: RichMBean, name: String, v: T){
    println(s"${formatMBeanName(mbean)}.${name} ${v},procName=${procName}${getTagString(mbean)}")
  }

  private def formatMBeanName(mbean: RichMBean): String = {
    val typeStr = Option(mbean.objectName.getKeyProperty("type")).map{ "." + _ }.getOrElse("")
    "jmx." + mbean.domain + typeStr
  }

  private def getTagString(mbean: RichMBean): String = {
    Option(mbean.objectName.getKeyProperty("name")).map{ ",name=" + _ }.getOrElse("")
  }

  private def getProcessName(conn: VMConnection) = {
    conn.jmx.systemProperties.getOrElse("tjmx.procId", conn.vm.name)
  }

}
