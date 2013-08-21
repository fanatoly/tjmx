package tjmx

import java.lang.management.ManagementFactory
import javax.management.ObjectName

trait TJmxStatsMBean{
  def getConnectionCount: Long
  def getBlacklistCount: Long
  def getLastSleepTime: Long
  def getNumMetrics: Long
  def getReadErrorCount: Long
  def getConnectErrorCount: Long
}


class TJmxStats extends TJmxStatsMBean{

  var connectionCount, blacklistCount, lastSleepTime, readErrorCount, connectErrorCount, numMetrics = 0L

  def getConnectionCount: Long = connectionCount
  def getBlacklistCount: Long = blacklistCount
  def getLastSleepTime: Long = lastSleepTime
  def getNumMetrics: Long = numMetrics
  def getReadErrorCount: Long = readErrorCount
  def getConnectErrorCount: Long = connectErrorCount
}


object TJmxStats{
  def apply() = {
    val mbeanSrv = ManagementFactory.getPlatformMBeanServer
    val name = new ObjectName("tjmx:type=TJmxStats")
    val stats = new TJmxStats
    mbeanSrv.registerMBean(stats, name)
    stats
  }
}
