#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# ===================================================================================================================
# In this Dockerfile, I am trying to install NDB from the binary per the docs (same process I used to install on VM).
# THIS IS THE WORKING DOCKERFILE.
# ===================================================================================================================

# Use AdoptOpenJDK's JDK8, OpenJ9, ubuntu for non-profiling.
# FROM adoptopenjdk/openjdk8-openj9:x86_64-ubuntu-jdk8u262-b10_openj9-0.21.0
FROM adoptopenjdk/openjdk8:x86_64-ubuntu-jdk8u292-b10

# Install various dependencies.
RUN rm -rf /var/lib/apt/lists/* \
    && apt-get clean \
    && apt-get update \
    && apt-get -y --no-install-recommends upgrade \
    && apt-get -y --no-install-recommends install wget \
    && apt-get -y --no-install-recommends install unzip \
    && apt-get -y --no-install-recommends install locales \
    && apt-get -y install libatomic1 \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/* \
    && locale-gen en_US.UTF-8

#EXPOSE 50010

# Set some environment variables.
# Much of this is done by-default. 
ENV LANG="en_US.UTF-8" \
	LANGUAGE="en_US:en" \
	LC_ALL="en_US.UTF-8" \
	VERSION=8 \
	UPDATE=222 \
	BUILD=10 \
   __OW_ALLOW_CONCURRENT="true" \
   LIBNDBPATH=/native/ \
   LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$LIBNDBPATH:/native/ \
   HADOOP_CONF_DIR="/conf/" \
   __ACTION_MEMORY=7500

ADD proxy /javaAction

# This is the configuration file for the NDB database. This is used by the serverless namenode.
ADD ndb-config.properties /metadata-dal/ndb-config.properties

# Add contents of the conf/ directory to conf/
ADD conf /conf/

# Add the contents of the native/ directory to native/.
# These would be the custom-built native hadoop libraries.
ADD native /native/

# Add contents of the libs/ directory to java_runtime_dependencies/
ADD libs /java_runtime_dependencies/

###############################
# YourKit Java Profiler Setup #
###############################

# Install YourKit Java Profiler agents:
#RUN wget https://www.yourkit.com/download/docker/YourKit-JavaProfiler-2022.3-docker.zip -P /tmp/ && \
#  unzip /tmp/YourKit-JavaProfiler-2022.3-docker.zip -d /usr/local && \
#  rm /tmp/YourKit-JavaProfiler-2022.3-docker.zip

# From default OpenWhisk image.
RUN cd /javaAction \
	&& rm -rf .classpath .gitignore .gradle .project .settings Dockerfile build \
	&& ./gradlew oneJar \
	&& rm -rf /javaAction/src \
	&& ./compileClassCache.sh

# Profiler ports + MySQL.
EXPOSE 1186 2202 3306 9010 10001

# Used with OpenJ9 JDK. Does not support any sort of profiling.
# CMD ["java", "-Dfile.encoding=UTF-8", "-Xshareclasses:cacheDir=/javaSharedCache,readonly", "-Xquickstart", "-Dcom.sun.management.jmxremote", "-Dcom.sun.management.jmxremote.port=9010", "-Dcom.sun.management.jmxremote.local.only=false", "-Dcom.sun.management.jmxremote.authenticate=false", "-Dcom.sun.management.jmxremote.ssl=false", "-Dcom.sun.management.jmxremote.rmi.port=9010", "-Djava.rmi.server.hostname=localhost", "-Djava.library.path=/native/", "-Dsun.io.serialization.extendedDebugInfo=true", "-Dlog4j.configuration=file:/conf/log4j.properties", "-cp", "/javaAction/build/libs/javaAction-all.jar", "org.apache.openwhisk.runtime.java.action.Proxy"]

# Used with Non-OpenJ9 JDK.
# Does NOT support any type of profiling.
CMD ["java", "-Dfile.encoding=UTF-8",                                       \
             "-Djava.library.path=/native/",                                \
             "-Dsun.io.serialization.extendedDebugInfo=true",               \
             "-Dlog4j.configuration=file:/conf/log4j.properties",           \
	         "-XX:+UseConcMarkSweepGC",					                    \
             # Max heap size, set the same as min heap size.
             "-Xmx7000m",                                                   \
             # Min heap size, set the same as max heap size.
             "-Xms7000m",                                                   \
             # Throw error if we spend too much time GC-ing.
             # Useful to detect this type of issue.
             # "-XX:-UseGCOverheadLimit",                                     \
             # Prevent usage of GC heuristics so GC is more predictable.
             # "-XX:+UseCMSInitiatingOccupancyOnly",                          \
             # Informs JVM when CMS should be triggered. We cache a lot of objects, so I've set it a little higher.
             # "-XX:CMSInitiatingOccupancyFraction=75",                       \
             # Instructs GC to collect young gen first before Full GC or CMS remark phase.
             # This improves performance due to no need to check ref between young gen and tenured.
             "-XX:+CMSScavengeBeforeRemark",                                \
             "-XX:+ScavengeBeforeFullGC",                                   \
             "-verbose:gc",                                                 \
             "-XX:+PrintGCTimeStamps",                                      \
             "-XX:+PrintGCDetails",                                         \
             "-XX:MaxNewSize=5000m",                                        \
             "-XX:+CMSClassUnloadingEnabled",                               \
             "-XX:MaxGCPauseMillis=350",                                    \
             #"-XX:+PrintTenuringDistribution",			                    \
             #"-XX:+PrintGCApplicationStoppedTime",                          \
             "-XX:MaxTenuringThreshold=2",				                    \
             "-XX:+UnlockDiagnosticVMOptions",                              \
             #"-XX:+PrintSafepointStatistics",                               \
             #"-XX:PrintSafepointStatisticsCount=1",                         \
             #"-XX:+SafepointTimeout",                                       \
             #"-XX:SafepointTimeoutDelay=50",                                \
             "-XX:ParGCCardsPerStrideChunk=2048",		 	                \
             #"-agentpath:/usr/local/YourKit-JavaProfiler-2022.3/bin/linux-x86-64/libyjpagent.so=port=10001,listen=all", \
             "-cp", "/javaAction/build/libs/javaAction-all.jar", "org.apache.openwhisk.runtime.java.action.Proxy"]

# Used with Non-OpenJ9 JDK.
# Supports profiling with VisualVM/JConsole (or any profiler that uses JMX and doesn't require anything more).
#CMD ["java", "-Dfile.encoding=UTF-8",                                       \
#             "-Dcom.sun.management.jmxremote",                              \
#             "-Dcom.sun.management.jmxremote.port=9010",                    \
#             "-Dcom.sun.management.jmxremote.local.only=false",             \
#             "-Dcom.sun.management.jmxremote.authenticate=false",           \
#             "-Dcom.sun.management.jmxremote.ssl=false",                    \
#             "-Dcom.sun.management.jmxremote.rmi.port=9010",                \
#             "-Djava.rmi.server.hostname=localhost",                        \
#             "-Djava.library.path=/native/",                                \
#             "-Dsun.io.serialization.extendedDebugInfo=true",               \
#             "-Dlog4j.configuration=file:/conf/log4j.properties",           \
#             "-cp", "/javaAction/build/libs/javaAction-all.jar", "org.apache.openwhisk.runtime.java.action.Proxy"]

# Used with Non-OpenJ9 JDK.
# Supports profiling with the YourKit Java profiler.
#CMD ["java", "-Dfile.encoding=UTF-8",                                       \
#             "-agentpath:/usr/local/YourKit-JavaProfiler-2021.11/bin/linux-x86-64/libyjpagent.so=port=10001,listen=all", \
#             "-Djava.library.path=/native/",                                \
#             "-Dsun.io.serialization.extendedDebugInfo=true",               \
#             "-Dlog4j.configuration=file:/conf/log4j.properties",           \
#             "-cp", "/javaAction/build/libs/javaAction-all.jar", "org.apache.openwhisk.runtime.java.action.Proxy"]
