import argparse
import os
import subprocess

#####################################
# Deploy Nuclio NameNode functions. #
#####################################


parser = argparse.ArgumentParser()

parser.add_argument("-d", "--directory", type = str, default = "./function-configurations/", help = "Input directory.")
parser.add_argument("-s", "--start", type = int, default = 0, help = "Starting index of YAML to to generate (inclusive).")
parser.add_argument("-e", "--end", type = int, default = 10, help = "Ending index of YAML to generate (exclusive).")

args = parser.parse_args()

start = args.start
end = args.end
directory = args.directory

if (end < start):
    raise ValueError("The ending index (%d) must be >= the starting index (%d)." % (end, start))

print("Deploying namenode%d through namenode%d (inclusive). Configuration files in directory '%s'." % (start, end-1, directory))

for i in range(start, end):
    config_file_path = os.path.join(directory, "namenode%d.yaml" % i)

    if not os.path.exists(config_file_path):
        print("[ERROR] File '%s' does not exist. Cannot deploy NameNode%d. Skipping..." % (config_file_path, i))
        continue

    cmd = ["nuctl", "deploy", "namenode%d" % i, "--file", config_file_path]
    print("Executing command: %s" % str(cmd))
    subprocess.run(["nuctl", "deploy", "namenode%d" % i, "--file", config_file_path])