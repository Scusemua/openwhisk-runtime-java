import argparse
import os

######################################################
# Generate Nuclio YAML files for NameNode functions. #
######################################################


parser = argparse.ArgumentParser()

parser.add_argument("-d", "--directory", type = str, default = "./function-configurations/", help = "Output directory.")
parser.add_argument("-o", "--overwrite", type = str, default = False, help = "Overwrite existing .YAML files.")
parser.add_argument("-s", "--start", type = int, default = 0, help = "Starting index of YAML to to generate (inclusive).")
parser.add_argument("-e", "--end", type = int, default = 10, help = "Ending index of YAML to generate (exclusive).")

YAML_TEMPLATE = \
"""
apiVersion: "nuclio.io/v1"
kind: NuclioFunction
metadata:
  name: namenode%d
spec:
  image: scusemua/java9-nuclio:latest
  runtime: java
  handler: org.apache.hadoop.hdfs.serverless.NuclioHandler
  replicas: 0

  resources:
    requests:
      cpu: 0.5
      memory: 1024M
    limits:
      cpu: 1.5
      memory: 1280M
  env:
    - name: FUNCTION_NAME
      value: namenode%d
  platform:
    healthCheck:
      enabled: false
  runtimeAttributes:
    jvmOptions:
      - "-Dlog4j.debug"
      - "-Djava.library.path=/native/"
      - "-Dsun.io.serialization.extendedDebugInfo=true"
      - "-Dlog4j.configuration=file:/conf/log4j.properties"
"""

args = parser.parse_args()

start = args.start
end = args.end
overwrite = args.overwrite
directory = args.directory

if (end < start):
    raise ValueError("The ending index (%d) must be >= the starting index (%d)." % (end, start))

print("Generating YAML files for NameNodes %d through %d (inclusive). Overwrite: %s. Output directory: '%s'" % (start, end-1, overwrite, directory))

for i in range(start, end):
    current_filename = os.path.join(directory, "namenode%d.yaml" % i)

    if not overwrite and os.path.exists(current_filename):
        print("File '%s' already exists and overwrite is FALSE. Skipping..." % current_filename)
        continue

    with open(current_filename, 'w') as f:
        print("Generating file '%s' now..." % current_filename)
        f.write(YAML_TEMPLATE % (i,i))