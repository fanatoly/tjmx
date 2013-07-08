TJMX
====

This utility is intented to be run as part of the tcollector framework. It scans the local machine for JVMs, connects to them via JMX, pulls out metrics and pipes them to tcollector.

## Example ##

`
/usr/bin/sudo -u tomcat7 JVM_OPT="-Xbootclasspath/a:/usr/lib/jvm/jdk1.7.0/lib/tools.jar" /home/fanatoly/workspace/tjmx/target/pack/bin/tjmx -q
`


## TODO ##
- Filters on JVMS and Metrics
- Exclusion filters
- More resiliance testing
