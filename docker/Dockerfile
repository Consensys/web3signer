# syntax=docker/dockerfile:1@sha256:2780b5c3bab67f1f76c781860de469442999ed1a0d7992a5efdf2cffc0e3d769

# Stage 1: Validate and extract application
FROM alpine@sha256:5b10f432ef3da1b8d4c7eb6c487f2f5a8f096bc91145e68878dd4a5019afde11 AS app-extract
ARG TAR_FILE
RUN test -n "$TAR_FILE" || (echo "ERROR: TAR_FILE build argument is required" && exit 1)
COPY ${TAR_FILE} /tmp/web3signer.tar.gz
RUN gzip -t /tmp/web3signer.tar.gz || (echo "ERROR: TAR_FILE is not a valid gzip archive" && exit 1) && \
    mkdir -p /opt/web3signer && \
    tar -xzf /tmp/web3signer.tar.gz -C /opt/web3signer --strip-components=1 && \
    rm /tmp/web3signer.tar.gz

# Stage 2: Final image with Ubuntu base for latest security patches
FROM ubuntu:24.04@sha256:c4a8d5503dfb2a3eb8ab5f807da5bc69a85730fb49b5cfca2330194ebcc41c7b

ARG BUILD_DATE
ARG VCS_REF
ARG VERSION

LABEL org.opencontainers.image.created=$BUILD_DATE \
      org.opencontainers.image.title="Web3Signer" \
      org.opencontainers.image.description="Ethereum 2.0 Signing Service" \
      org.opencontainers.image.url="https://docs.web3signer.consensys.net" \
      org.opencontainers.image.documentation="https://docs.web3signer.consensys.net" \
      org.opencontainers.image.source="https://github.com/Consensys/web3signer" \
      org.opencontainers.image.revision=$VCS_REF \
      org.opencontainers.image.version=$VERSION \
      org.opencontainers.image.vendor="Consensys" \
      org.opencontainers.image.licenses="Apache-2.0" \
      org.opencontainers.image.base.name="ubuntu:24.04"

ENV JAVA_HOME="/opt/java/openjdk" \
    PATH="/opt/java/openjdk/bin:${PATH}" \
    WEB3SIGNER_HTTP_LISTEN_HOST="0.0.0.0" \
    WEB3SIGNER_METRICS_HOST="0.0.0.0"

# Apply all security updates, install dependencies and create user
RUN apt-get update && \
    apt-get upgrade -y && \
    apt-get install -y --no-install-recommends \
        ca-certificates \
        curl && \
    rm -rf /var/lib/apt/lists/* && \
    useradd --system --no-create-home --shell /bin/false web3signer

# Copy full JRE from Eclipse Temurin (no module complexity, all features available)
COPY --from=eclipse-temurin:25.0.2_10-jre@sha256:1089975c9ab22e822faf568c0e03997ee707165a125b7a55fbc799315b63d697 /opt/java/openjdk /opt/java/openjdk
COPY --from=app-extract --chown=web3signer:web3signer /opt/web3signer /opt/web3signer

USER web3signer
WORKDIR /opt/web3signer

EXPOSE 9000 9001

ENTRYPOINT ["/opt/web3signer/bin/web3signer"]
