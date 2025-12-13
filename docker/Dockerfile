# syntax=docker/dockerfile:1

# Stage 1: Validate and extract application
FROM alpine AS app-extract
ARG TAR_FILE
RUN test -n "$TAR_FILE" || (echo "ERROR: TAR_FILE build argument is required" && exit 1)
COPY ${TAR_FILE} /tmp/web3signer.tar.gz
RUN gzip -t /tmp/web3signer.tar.gz || (echo "ERROR: TAR_FILE is not a valid gzip archive" && exit 1) && \
    mkdir -p /opt/web3signer && \
    tar -xzf /tmp/web3signer.tar.gz -C /opt/web3signer --strip-components=1 && \
    rm /tmp/web3signer.tar.gz

# Stage 2: Final image
FROM ubuntu:24.04

ARG BUILD_DATE
ARG VCS_REF
ARG VERSION

LABEL org.label-schema.build-date=$BUILD_DATE \
      org.label-schema.name="Web3Signer" \
      org.label-schema.description="Ethereum 2.0 Signing Service" \
      org.label-schema.url="https://docs.web3signer.consensys.net" \
      org.label-schema.vcs-ref=$VCS_REF \
      org.label-schema.vcs-url="https://github.com/ConsenSys/web3signer.git" \
      org.label-schema.vendor="Consensys" \
      org.label-schema.version=$VERSION \
      org.label-schema.schema-version="1.0"

ENV JAVA_HOME="/opt/java/openjdk" \
    PATH="/opt/java/openjdk/bin:${PATH}" \
    WEB3SIGNER_HTTP_LISTEN_HOST="0.0.0.0" \
    WEB3SIGNER_METRICS_HOST="0.0.0.0"

# Install dependencies and create user first (before copying files)
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
        ca-certificates \
        curl && \
    rm -rf /var/lib/apt/lists/* && \
    useradd --system --no-create-home --shell /bin/false web3signer

# Copy JRE from the Eclipse Termuin docker image
COPY --from=eclipse-temurin:25-jre /opt/java/openjdk /opt/java/openjdk
COPY --from=app-extract --chown=web3signer:web3signer /opt/web3signer /opt/web3signer

USER web3signer
WORKDIR /opt/web3signer

EXPOSE 9000 9001

ENTRYPOINT ["/opt/web3signer/bin/web3signer"]

