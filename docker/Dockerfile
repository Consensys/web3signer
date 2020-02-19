
FROM openjdk:11.0.2-jre-slim-stretch

COPY eth2signer /opt/eth2signer/
WORKDIR /opt/eth2signer

# Expose services ports
# 9000 Signer endpoint
EXPOSE 9000

ENTRYPOINT ["/opt/eth2signer/bin/eth2signer"]

# Build-time metadata as defined at http://label-schema.org
ARG BUILD_DATE
ARG VCS_REF
ARG VERSION
LABEL org.label-schema.build-date=$BUILD_DATE \
      org.label-schema.name="Eth2signer" \
      org.label-schema.description="Ethereum2 artefact signing application" \
      org.label-schema.url="https://docs.eth2signer.pegasys.tech/" \
      org.label-schema.vcs-ref=$VCS_REF \
      org.label-schema.vcs-url="https://github.com/PegaSysEng/eth2signer.git" \
      org.label-schema.vendor="Pegasys" \
      org.label-schema.version=$VERSION \
      org.label-schema.schema-version="1.0"
