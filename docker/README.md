To build docker image on developer box, run the following commands (from the root of the project):

```sh
./gradlew distTar

docker build --no-cache --pull --build-arg TAR_FILE=./build/distributions/web3signer-develop.tar.gz \
-f ./docker/Dockerfile -t web3signer:develop .

docker run --rm -it web3signer:develop --version
```