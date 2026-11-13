FROM ghcr.io/graalvm/native-image-community:25 AS build

WORKDIR /build
COPY . .
RUN ./gradlew --no-daemon :java-operator:nativeCompile

FROM eclipse-temurin:25-jdk AS build-debug

WORKDIR /build
COPY . .
RUN ./gradlew --no-daemon :java-operator:buildLayers

# For -debug images only: the operator on a JVM, with a debugger listening on port 5005.
FROM eclipse-temurin:25-jre AS debug

COPY --from=build-debug /build/java-operator/build/docker/main/layers/libs /deployments/libs
COPY --from=build-debug /build/java-operator/build/docker/main/layers/resources /deployments/resources
COPY --from=build-debug /build/java-operator/build/docker/main/layers/app/application.jar /deployments/application.jar

ENV MICRONAUT_CONFIG_FILES=/deployments/config/application.properties \
    HOME=/tmp

USER 1000:1000

ENTRYPOINT ["java", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005", "-jar", "/deployments/application.jar"]

FROM gcr.io/distroless/base-debian12:nonroot

COPY --from=build /build/java-operator/build/native/nativeCompile/xp-operator /deployments/xp-operator

ENV MICRONAUT_CONFIG_FILES=/deployments/config/application.properties \
    HOME=/tmp

USER 1000:1000

ENTRYPOINT ["/deployments/xp-operator"]
