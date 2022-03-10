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

ARG NUCLIO_LABEL=1.7.11
ARG NUCLIO_ARCH=amd64
ARG NUCLIO_BASE_IMAGE=openjdk:9-jre-slim
ARG NUCLIO_DOCKER_REPO=quay.io/nuclio
ARG NUCLIO_ONBUILD_IMAGE=handler-builder-java-onbuild:${NUCLIO_LABEL}-${NUCLIO_ARCH}

# Supplies processor, handler.jar
FROM ${NUCLIO_DOCKER_REPO}/${NUCLIO_ONBUILD_IMAGE} as builder

# From the base image
FROM ${NUCLIO_BASE_IMAGE}

# Copy required objects from the suppliers
COPY --from=builder /home/gradle/bin/processor /usr/local/bin/processor
COPY --from=builder /home/gradle/src/wrapper/build/libs/nuclio-java-wrapper.jar /opt/nuclio/nuclio-java-wrapper.jar

# Set some environment variables.
# Much of this is done by-default. 
ENV LIBNDBPATH=/native/ \
    LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$LIBNDBPATH:/native/ \
    HADOOP_CONF_DIR="/conf/"

# This is the configuration file for the NDB database. This is used by the serverless namenode.
ADD ndb-config.properties /metadata-dal/ndb-config.properties

# Add contents of the conf/ directory to conf/
ADD conf /conf/

# Extra dependencies that we'll add at runtime.
# ADD libs /additional_java_libs/

# Add the contents of the native/ directory to native/.
# These would be the custom-built native hadoop libraries.
ADD native /native/

# Run processor with configuration and platform configuration
CMD [ "processor" ]
