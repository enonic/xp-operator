# Inspired by https://github.com/fabric8io-images/java/blob/master/images/alpine/openjdk11/jre/Dockerfile

## from gradle
FROM gradle:8.3.0-jdk11 AS build
## install helm in ubuntu
COPY --from=alpine/helm:3.11.3 /usr/bin/helm /usr/bin/helm

WORKDIR /home/gradle/project
COPY . .
RUN gradle build -x check

FROM eclipse-temurin:11-jre AS runtime

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

# Install helm
COPY --from=alpine/helm:3.11.3 /usr/bin/helm /usr/bin/helm

# Set ENV vars
ENV JAVA_OPTIONS="-Doperator.charts.path=helm -Djava.util.logging.manager=org.jboss.logmanager.LogManager" \
    AB_ENABLED=jmx_exporter

# Add run script as /deployments/run-java.sh
COPY docker/run-java.sh /deployments/

# Copy helm charts
COPY --chown=1000:1000 java-operator/src/main/helm /deployments/helm/

# Copy build target
COPY --from=build --chown=1000:1000 /home/gradle/project/java-operator/build/quarkus-app /deployments/

CMD [ "/deployments/run-java.sh" ]
