#!/bin/bash

set -e

export GOSS_PATH=tests/goss-linux-amd64
export GOSS_OPTS="$GOSS_OPTS --format junit"
export GOSS_FILES_STRATEGY=cp

DOCKER_IMAGE=$1
DOCKER_TEST_IMAGE=ethsigner_goss

# create test docker image that includes the test key file and password files
TEST_CONTAINER_ID=$(docker create ${DOCKER_IMAGE})
docker cp ./tests/test_keyfile.json ${TEST_CONTAINER_ID}:/tmp/test_keyfile.json
docker cp ./tests/test_password ${TEST_CONTAINER_ID}:/tmp/test_password
docker commit ${TEST_CONTAINER_ID} ${DOCKER_TEST_IMAGE}

i=0

# Test for normal startup with ports opened
GOSS_FILES_PATH=tests/01 \
bash tests/dgoss \
run ${DOCKER_TEST_IMAGE} \
--chain-id=2018 \
--http-listen-host=0.0.0.0 \
--downstream-http-port=8590 \
file-based-signer \
--key-file /tmp/test_keyfile.json \
--password-file /tmp/test_password \
> ./reports/01.xml || i=`expr $i + 1`

docker image rm ${DOCKER_TEST_IMAGE}

exit $i
