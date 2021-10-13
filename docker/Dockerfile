# Inspired by https://github.com/fabric8io-images/java/blob/master/images/alpine/openjdk11/jre/Dockerfile

FROM adoptopenjdk:11-jre-hotspot

RUN mkdir -p /deployments

# JAVA_APP_DIR is used by run-java.sh for finding the binaries
ENV JAVA_APP_DIR=/deployments \
    JAVA_MAJOR_VERSION=11

# /dev/urandom is used as random source, which is perfectly safe
# according to http://www.2uo.de/myths-about-urandom/
RUN echo "securerandom.source=file:/dev/urandom" >>/opt/java/openjdk/conf/security/java.security

# Agent bond including Jolokia and jmx_exporter
COPY docker/agent-bond-opts /opt/run-java-options
RUN mkdir -p /opt/agent-bond && \
    curl https://repo.maven.apache.org/maven2/io/fabric8/agent-bond-agent/1.2.0/agent-bond-agent-1.2.0.jar -o /opt/agent-bond/agent-bond.jar && \
    chmod 444 /opt/agent-bond/agent-bond.jar && \
    chmod 755 /opt/run-java-options
COPY docker/jmx_exporter_config.yml /opt/agent-bond/
EXPOSE 8778 9779

# Add run script as /deployments/run-java.sh
COPY docker/run-java.sh /deployments/

# Install helm
COPY --from=alpine/helm:3.7.0 /usr/bin/helm /usr/bin/helm

# Set ENV vars
ENV JAVA_OPTIONS="-Doperator.charts.path=helm -Djava.util.logging.manager=org.jboss.logmanager.LogManager" \
    AB_ENABLED=jmx_exporter

# Copy helm charts
COPY --chown=1000:1000 java-operator/src/main/helm /deployments/helm

# Copy build target
COPY --chown=1000:1000 java-operator/target/quarkus-app /tmp/quarkus-app
RUN mv /tmp/quarkus-app/* /deployments

CMD [ "/deployments/run-java.sh" ]
