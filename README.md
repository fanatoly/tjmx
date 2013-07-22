TJMX
====

This utility is intented to be run as part of the tcollector framework. It scans the local machine for JVMs, connects to them via JMX, pulls out metrics and pipes them to tcollector.

## Usage ##
As mentinoed above the TJMX utity is meant to be run as from within the tcollector framework. Usually, this entails dropping a runner script into the collectors directory watched by tcollector. An example of such a script is provided under scripts.

The runner script is responsible for suplying TJMX with a few pieces of information

* regexes for filtering which JVMs are monitored, as well as which metrics are output.
* the location of the tools.jar library that should be used. This is supplied as a `-Xbootclasspath/a:` parameter
* The username that the TJMX should run as. This is particularly important as it is not suggested to run a local JMX client as a user different than that of the monitored process.

Most java processes should be set up out of the box - with one notable exception. If you have more than 1 process accessible that is running the same main class/has the same JPS name, you will need to disambiguate. This is done by defining `tjmx.procId` in your process. This will be used to override the JPS process name. You can

## Example ##

`
/usr/bin/sudo -u tomcat7 JVM_OPT="-Xbootclasspath/a:/usr/lib/jvm/jdk1.7.0/lib/tools.jar" /home/fanatoly/workspace/tjmx/target/pack/bin/tjmx
`


## TODO ##
- Exclusion filters
- More resiliance testing
