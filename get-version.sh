#!/usr/bin/env bash

exec ./gradlew properties --console=plain -q | grep "^version:" | awk '{printf $2}'
