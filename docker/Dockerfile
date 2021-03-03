FROM fabric8/java-centos-openjdk11-jre:1.8

# Install helm
COPY --from=alpine/helm:3.5.2 /usr/bin/helm /usr/bin/helm

# Set ENV vars
ENV JAVA_OPTIONS="-Doperator.charts.path=helm -Djava.util.logging.manager=org.jboss.logmanager.LogManager" \
    AB_ENABLED=jmx_exporter

# Copy helm charts
COPY java-operator/src/main/helm /deployments/helm

# Copy build target
COPY java-operator/target/*-runner.jar /deployments/operator.jar

CMD [ "/deployments/run-java.sh" ]