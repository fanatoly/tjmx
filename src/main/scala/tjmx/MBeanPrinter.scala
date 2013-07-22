package tjmx

import fr.janalyse.jmx._
import scala.util.matching.Regex
import scala.collection.JavaConversions._


class MBeanPrinter(filter: Regex){

  def output(jmx: JMX, vm: VM){
    val procName = getProcessName(jmx, vm)
    val ts = System.currentTimeMillis/1000
    jmx.mbeans.foreach{ mbean =>
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
    val output = s"${formatMBeanName(mbean)}.${name} ${ts} ${v} procName=${formatVal(procName)} ${getTagString(mbean)}"
    if(filter.findFirstIn(output).isDefined)
      println(output)
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

  private def getProcessName(jmx: JMX, vm: VM) = {
    jmx.systemProperties.getOrElse("tjmx.procId", vm.name)
  }

}
