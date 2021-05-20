FROM fabric8/java-alpine-openjdk11-jre:1.8

# Install helm
COPY --from=alpine/helm:3.5.4 /usr/bin/helm /usr/bin/helm

# Set ENV vars
ENV JAVA_OPTIONS="-Doperator.charts.path=helm -Djava.util.logging.manager=org.jboss.logmanager.LogManager" \
    AB_ENABLED=jmx_exporter

# Copy helm charts
COPY --chown=1000:1000 java-operator/src/main/helm /deployments/helm

# Copy build target
COPY --chown=1000:1000 java-operator/target/quarkus-app /tmp/quarkus-app
RUN mv /tmp/quarkus-app/* /deployments

CMD [ "/deployments/run-java.sh" ]
