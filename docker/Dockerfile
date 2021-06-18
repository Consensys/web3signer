
FROM adoptopenjdk/openjdk11:x86_64-ubuntu-jre-11.0.11_9

COPY web3signer /opt/web3signer/
WORKDIR /opt/web3signer

# Expose services ports
# 9000 Signer endpoint
EXPOSE 9000

ENTRYPOINT ["/opt/web3signer/bin/web3signer"]

# Build-time metadata as defined at http://label-schema.org
ARG BUILD_DATE
ARG VCS_REF
ARG VERSION
LABEL org.label-schema.build-date=$BUILD_DATE \
      org.label-schema.name="Web3Signer" \
      org.label-schema.description="Ethereum2 artefact signing application" \
      org.label-schema.url="https://docs.web3signer.pegasys.tech/" \
      org.label-schema.vcs-ref=$VCS_REF \
      org.label-schema.vcs-url="https://github.com/ConsenSys/web3signer.git" \
      org.label-schema.vendor="Pegasys" \
      org.label-schema.version=$VERSION \
      org.label-schema.schema-version="1.0"
