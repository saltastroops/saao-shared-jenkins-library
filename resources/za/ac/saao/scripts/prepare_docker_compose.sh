#!/bin/bash

# Prepare the docker compose file and output the content of the generated file.
#
# The following arguments need to be passed to the script, in this order:
#
# 1. The path of the docker compose file, such as docker-compose.yml.
# 2. The container registry URL such as https://registry.example.com.
# 3. The container registry username.
# 4. The Docker image name, such as my-great-webapp.
# 5. The Docker image tag.

# The docker registry variable should have an "https://" or "http://" prefix, but this
# must not be included in the pushed image name.
registry=$(echo "$2" | sed s#https://## | sed s#http://##)

# Replace the ${REGISTRY}, ${REGISTRY_USERNAME}, ${IMAGE_NAME} and ${TAG} environment
# variables in the docker compose file with the correct values, and output the result to
# stdout.
# Note that the pound sign is used as the delimiter for the first sed pattern, as the
# replacement value (a URL) contains slashes.
registry_pattern='s#${REGISTRY}#'
registry_pattern+="${registry}#"
username_pattern='s/${REGISTRY_USERNAME}/'
username_pattern+="$3/"
image_name_pattern='s/${IMAGE_NAME}/'
image_name_pattern+="$4/"
tag_pattern='s/${TAG}/'
tag_pattern+="$5/"
sed "$registry_pattern" "$1" | sed "$username_pattern" | sed "$image_name_pattern" | sed "$tag_pattern"
