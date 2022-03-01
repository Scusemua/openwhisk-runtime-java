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

ARG NUCLIO_LABEL=0.7.1
ARG NUCLIO_ARCH=amd64
ARG NUCLIO_BASE_IMAGE=openjdk:9-jre-slim
ARG NUCLIO_ONBUILD_IMAGE=nuclio/handler-builder-java-onbuild:${NUCLIO_LABEL}-${NUCLIO_ARCH}

# Supplies processor, handler.jar
FROM ${NUCLIO_ONBUILD_IMAGE} as builder

# Supplies uhttpc, used for healthcheck
FROM nuclio/uhttpc:0.0.1-amd64 as uhttpc

# From the base image
FROM ${NUCLIO_BASE_IMAGE}

# Copy required objects from the suppliers
COPY --from=builder /home/gradle/bin/processor /usr/local/bin/processor
COPY --from=builder /home/gradle/src/wrapper/build/libs/nuclio-java-wrapper.jar /opt/nuclio/nuclio-java-wrapper.jar
COPY --from=uhttpc /home/nuclio/bin/uhttpc /usr/local/bin/uhttpc

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
   CLASSPATH="./:/java_runtime_dependencies/*"

# This is the configuration file for the NDB database. This is used by the serverless namenode.
ADD ndb-config.properties /metadata-dal/ndb-config.properties

# Add contents of the conf/ directory to conf/
ADD conf /conf/

# Add the contents of the native/ directory to native/.
# These would be the custom-built native hadoop libraries.
ADD native /native/

# Add contents of the libs/ directory to java_runtime_dependencies/
ADD libs /java_runtime_dependencies/

# Readiness probe
HEALTHCHECK --interval=1s --timeout=3s CMD /usr/local/bin/uhttpc --url http://127.0.0.1:8082/ready || exit 1

# Run processor with configuration and platform configuration
CMD [ "processor" ]