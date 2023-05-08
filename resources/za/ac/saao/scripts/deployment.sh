#!/bin/bash

# Script for deploying.
#
# The following arguments need to be passed to the script, in this order:
#
# 1. The container registry URLm such as https://registry.example.com.
# 2. The container registry username.
# 3. The Docker image name, such as my-great-webapp.
# 4. The Docker image tag.

# Pull the latest docker image and restart the service defined in the docker compose
# file.

# Log into the docker registry
cat registry-password.txt | docker login --password-stdin -u "$2" "$1"

# Pull the docker image for the service, but don't restart the service
docker compose pull

# Log out from the Docker registry again
docker logout "$1"

# Restart the service
docker compose down || true
docker compose up -d

# Clean up
docker image prune
