# Inspired by https://github.com/fabric8io-images/java/blob/master/images/alpine/openjdk11/jre/Dockerfile

FROM eclipse-temurin:25-jre

# JAVA_APP_DIR is used by run-java.sh for finding the binaries
ENV JAVA_APP_DIR=/deployments \
    JAVA_MAJOR_VERSION=25

# /dev/urandom is used as random source, which is perfectly safe
# according to http://www.2uo.de/myths-about-urandom/
RUN echo "securerandom.source=file:/dev/urandom" >>/opt/java/openjdk/conf/security/java.security

# Agent bond including Jolokia and jmx_exporter.
ADD https://repo.maven.apache.org/maven2/io/fabric8/agent-bond-agent/1.2.0/agent-bond-agent-1.2.0.jar /opt/agent-bond/agent-bond.jar
COPY docker/agent-bond-opts /opt/run-java-options
RUN chmod 444 /opt/agent-bond/agent-bond.jar && \
    chmod 755 /opt/run-java-options
COPY docker/jmx_exporter_config.yml /opt/agent-bond/
EXPOSE 8778 9779

# Install helm
COPY --from=alpine/helm:3.11.3 /usr/bin/helm /usr/bin/helm

# Set ENV vars
ENV JAVA_OPTIONS="-Doperator.charts.path=helm -Dmicronaut.config.files=/deployments/config/application.properties" \
    AB_ENABLED=jmx_exporter

# Add run script as /deployments/run-java.sh
COPY docker/run-java.sh /deployments/

# Copy helm charts
COPY --chown=1000:1000 java-operator/src/main/helm /deployments/helm/

# Copy build target
COPY --chown=1000:1000 java-operator/build/docker/main/layers/app/application.jar /deployments/
COPY --chown=1000:1000 java-operator/build/docker/main/layers/libs /deployments/libs/
COPY --chown=1000:1000 java-operator/build/docker/main/layers/resources /deployments/resources/

# Ensure run-java.sh is executable regardless of host fs mode
RUN chmod +x /deployments/run-java.sh

ENV HOME=/tmp
USER 1000:1000

CMD [ "/deployments/run-java.sh" ]
