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

# Use AdoptOpenJDK's JDK8, OpenJ9, ubuntu
FROM adoptopenjdk/openjdk8-openj9:x86_64-ubuntu-jdk8u262-b10_openj9-0.21.0

# Install various dependencies.
RUN rm -rf /var/lib/apt/lists/* \
    && apt-get clean \
    && apt-get update \
    && apt-get -y --no-install-recommends upgrade \
    && apt-get -y --no-install-recommends install locales \
    && apt-get -y install libatomic1 \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/* \
    && locale-gen en_US.UTF-8

# Set some environment variables. 
# Much of this is done by-default. 
ENV LANG="en_US.UTF-8" \
	LANGUAGE="en_US:en" \
	LC_ALL="en_US.UTF-8" \
	VERSION=8 \
	UPDATE=222 \
	BUILD=10 \
   LIBNDBPATH=/usr/lib64 \
   LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$LIBNDBPATH \
   HADOOP_CONF_DIR="/conf/"

# Add the C++ NDB code to the image.
ADD libndbclient.so /usr/lib64/libndbclient.so

ADD proxy /javaAction

# This is the configuration file for the NDB database. This is used by the serverless namenode.
ADD ndb-config.properties /metadata-dal/ndb-config.properties

# This is the Metadata DAL NDB implementation. Used by serverless namenode to communicate with NDB.
ADD hops-metadata-dal-impl-ndb-3.2.0.3-SNAPSHOT-jar-with-dependencies.jar /metadata-dal/hops-metadata-dal-impl-ndb-3.2.0.3-SNAPSHOT-jar-with-dependencies.jar
#ADD hops-metadata-dal-3.2.0.3-SNAPSHOT.jar /metadata-dal/hops-metadata-dal-3.2.0.3-SNAPSHOT.jar

# This contains more required HopsFS/Hadoop code, but it is not necessarily specific to the serverless namenode or HDFS itself.
# COMMENTED OUT: I'm just moving hadoop-common to the libs/ directory.
# ADD hadoop-common-3.2.0.3-SNAPSHOT.jar /java_runtime_dependencies/hadoop-common-3.2.0.3-SNAPSHOT.jar

# Add contents of the conf/ directory to conf/
ADD conf /conf/

# Add contents of the libs/ directory to java_runtime_dependencies/
ADD libs /java_runtime_dependencies/

# From default OpenWhisk image.
RUN cd /javaAction \
	&& rm -rf .classpath .gitignore .gradle .project .settings Dockerfile build \
	&& ./gradlew oneJar \
	&& rm -rf /javaAction/src \
	&& ./compileClassCache.sh

# I added "-Xms512m" and "-Xmx1024m" to increase memory allocated to the JVM.
#CMD ["java", "-Dfile.encoding=UTF-8", "-Xms512m", "-Xmx1024m", "-Xshareclasses:cacheDir=/javaSharedCache,readonly", "-Xquickstart", "-cp", "/javaAction/build/libs/javaAction-all.jar:/metadata-dal/hops-metadata-dal-3.2.0.3-SNAPSHOT.jar:/javaAction/build/libs/javaAction-all.jar:/conf/:/java_runtime_dependencies/*", "-Djava.library.path=/usr/lib64/", "-Dsun.io.serialization.extendedDebugInfo=true","-Dlog4j.configuration=file:/conf/log4j.properties","org.apache.openwhisk.runtime.java.action.Proxy"]

CMD ["java", "-Dfile.encoding=UTF-8", "-Xms512m", "-Xmx1024m", "-Xshareclasses:cacheDir=/javaSharedCache,readonly", "-Xquickstart", "-cp", "/javaAction/build/libs/javaAction-all.jar", "-Djava.library.path=/usr/lib64/", "-Dsun.io.serialization.extendedDebugInfo=true","-Dlog4j.configuration=file:/conf/log4j.properties","org.apache.openwhisk.runtime.java.action.Proxy"]

# CMD ["java", "-Dfile.encoding=UTF-8", "-Xms512m", "-Xmx1024m", "-Xshareclasses:cacheDir=/javaSharedCache,readonly", "-Xquickstart", "-cp", "/metadata-dal/hops-metadata-dal-3.2.0.0-SNAPSHOT.jar:/javaAction/build/libs/javaAction-all.jar:/java_runtime_dependencies/*", "-Djava.library.path=/usr/lib64/", "org.apache.openwhisk.runtime.java.action.Proxy"]
