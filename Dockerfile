FROM ghcr.io/graalvm/native-image-community:25 AS build

WORKDIR /build
COPY . .
RUN ./gradlew --no-daemon :java-operator:nativeCompile

FROM gcr.io/distroless/base-debian12:nonroot

COPY --from=build /build/java-operator/build/native/nativeCompile/xp-operator /deployments/xp-operator

ENV MICRONAUT_CONFIG_FILES=/deployments/config/application.properties \
    HOME=/tmp

USER 1000:1000

ENTRYPOINT ["/deployments/xp-operator"]
