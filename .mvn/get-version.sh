#!/usr/bin/env bash

exec ./mvnw -f ./java-operator/pom.xml help:evaluate -Dexpression=project.version -q -DforceStdout