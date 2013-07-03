package tjmx

import fr.janalyse.jmx._
import scala.util.matching.Regex
import scala.collection.JavaConversions._


class MBeanPrinter(queries: List[Regex]){

  def output(conn: VMConnection){
    val procName = getProcessName(conn)
    val ts = System.currentTimeMillis/1000
    conn.jmx.mbeans.foreach{ mbean =>
      mbean.attributes.foreach{
        case dbl: RichNumberAttribute => mbean.getString(dbl).map{ printVal(procName, mbean, dbl.name, _, ts) }
        case composite: RichCompositeDataAttribute => mbean.getNumberComposite(composite).map { numMap =>
          numMap.foreach{ kv =>
            printVal(procName, mbean, composite.name + "." + kv._1, kv._2, ts)
          }
        }
        case _ =>
      }
    }
  }

  private def printVal[T](procName: String, mbean: RichMBean, name: String, v: T, ts: Long){
    println(s"${formatMBeanName(mbean)}.${name} ${ts} ${v} procName=${formatVal(procName)} ${getTagString(mbean)}")
  }

  private def formatVal(v: String) = {
    v.replace(' ', '_').replace("\"", "")
  }

  private def formatMBeanName(mbean: RichMBean): String = {
    val typeStr = Option(mbean.objectName.getKeyProperty("type")).map{ "." + _ }.getOrElse("")
    "jmx." + mbean.domain + typeStr
  }

  private def getTagString(mbean: RichMBean): String = {
    // Option(mbean.objectName.getKeyProperty("name")).map{ "name=" + _ }.getOrElse("")
    mbean.objectName.getKeyPropertyList.entrySet.foldLeft(""){ (prev, currEntry) =>
      if(currEntry.getKey() != "type")
        s"${prev}  ${currEntry.getKey()}=${formatVal(currEntry.getValue())}"
      else prev
    }
  }

  private def getProcessName(conn: VMConnection) = {
    conn.jmx.systemProperties.getOrElse("tjmx.procId", conn.vm.name)
  }

}
