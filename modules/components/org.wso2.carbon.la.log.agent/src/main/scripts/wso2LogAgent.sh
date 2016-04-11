#!/bin/bash
#---------------------------------#
# dynamically build the classpath #
#---------------------------------#
THE_CLASSPATH=
for i in `ls ../lib/*.jar`
do
  THE_CLASSPATH=${THE_CLASSPATH}:${i}
done
THE_CLASSPATH=${THE_CLASSPATH}:../config
java -cp ${THE_CLASSPATH} org.wso2.carbon.la.log.agent.LogAgentMain $@