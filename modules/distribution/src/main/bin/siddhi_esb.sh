#!/bin/sh

#   Licensed to the Apache Software Foundation (ASF) under one
#   or more contributor license agreements.  See the NOTICE file
#   distributed with this work for additional information
#   regarding copyright ownership.  The ASF licenses this file
#   to you under the Apache License, Version 2.0 (the
#   "License"); you may not use this file except in compliance
#   with the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
#   Unless required by applicable law or agreed to in writing,
#   software distributed under the License is distributed on an
#    #  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
#   KIND, either express or implied.  See the License for the
#   specific language governing permissions and limitations
#   under the License.

# -----------------------------------------------------------------------------
#
# Environment Variable Prerequisites
#
#   SIDDHI_ESB_HOME   Home of Siddhi ESB installation. If not set will use the parent directory
#
#   JAVA_HOME      Must point at your Java Development Kit installation.
#
# NOTE: Borrowed generously from Apache Tomcat startup scripts.

# if JAVA_HOME is not set we're not happy
if [ -z "$JAVA_HOME" ]; then
  echo "You must set the JAVA_HOME variable before running Siddhi ESB."
  exit 1
fi

# OS specific support.  $var _must_ be set to either true or false.
cygwin=false
os400=false
case "`uname`" in
CYGWIN*) cygwin=true;;
OS400*) os400=true;;
esac

# resolve links - $0 may be a softlink
PRG="$0"

while [ -h "$PRG" ]; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '.*/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "$PRG"`/"$link"
  fi
done

# Get standard environment variables
PRGDIR=`dirname "$PRG"`

# Only set SIDDHI_ESB_HOME if not already set
[ -z "$SIDDHI_ESB_HOME" ] && SIDDHI_ESB_HOME=`cd "$PRGDIR/.." ; pwd`


while [ $# -ge 1 ]; do

if [ "$1" = "-xdebug" ]; then
    XDEBUG="-Xdebug -Xnoagent -Xrunjdwp:transport=dt_socket,server=y,address=5005"
    shift


elif [ "$1" = "-h" ]; then
    echo "commands:"
    shift
    exit 0

  else
    echo "Error: unknown command:$1"
    shift
    exit 1
  fi

done


SIDDHI_ESB_CLASSPATH=$SIDDHI_ESB_CLASSPATH:"$SIDDHI_ESB_HOME/lib"
for f in $SIDDHI_ESB_HOME/lib/*.jar
do
  SIDDHI_ESB_CLASSPATH=$SIDDHI_ESB_CLASSPATH:$f
done
SIDDHI_ESB_CLASSPATH=$SIDDHI_ESB_HOME/repository/conf:$JAVA_HOME/lib/tools.jar:$SIDDHI_ESB_CLASSPATH:$CLASSPATH

# use proper bouncy castle version for the JDK
jdk_15=`$JAVA_HOME/bin/java -version 2>&1 | grep 1.5`


# ----- Execute The Requested Command -----------------------------------------

cd $SIDDHI_ESB_HOME
echo "Using SIDDHI_ESB_HOME:    $SIDDHI_ESB_HOME"
echo "Using JAVA_HOME:       $JAVA_HOME"

$JAVA_HOME/bin/java -server -Xms2048m -Xmx2048m -XX:MaxPermSize=1024m \
    $XDEBUG \
    $TEMP_PROPS \
    -Djava.io.tmpdir=$SIDDHI_ESB_HOME/work/temp/siddhi-esb \
    -classpath $SIDDHI_ESB_CLASSPATH \
    org.siddhiesb.controller.ESBController \
        $SIDDHI_ESB_HOME/repository \
        $SIDDHI_ESB_HOME \
        $SIDDHI_ESB_HOME/repository \
        $SERVER_NAME
