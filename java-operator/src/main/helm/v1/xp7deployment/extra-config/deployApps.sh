#!/bin/sh

echo "Bootstrap deploy folder apps"

DEPLOY_DIR="$1"

add() {
    URL="$1"
    NAME="$2"
    printf "Adding %s ... " "$NAME"
    if [ -f "$DEPLOY_DIR/$NAME" ]; then
        echo "skipped! Already exists!"
        exit
    fi

    fail=$(wget --quiet --output-document="$DEPLOY_DIR/$NAME" "$URL" 2>&1)
    if [ $? -ne 0 ]; then
        rm -f "$DEPLOY_DIR/$NAME"
        echo "failed!"
        echo "$fail"
        exit 1
    fi
    echo "success!"
}

remove() {
    NAME="$1"
    printf "Removing %s ... " "$NAME"
    if [ ! -f "$DEPLOY_DIR/$NAME" ]; then
        echo "skipped! Not found!"
        exit
    fi
    fail=$(rm "$DEPLOY_DIR/$NAME" 2>&1) || { echo "failed! "; echo "$fail"; exit 1; }
    echo "success!"
}

# Delete apps
for f in $(find "$DEPLOY_DIR" -name '*.jar'); do
  name=$(basename "$f")
  if [ "$(echo '{{ range $app := .Values.deployment.spec.nodesPreinstalledApps }}{{ $app.name }}-{{ trunc 10 (sha1sum $app.url) }}.jar{{ end }}' | grep "$name")" = "" ]; then
    remove "$name"
  fi
done

# Add apps
{{- range $app := .Values.deployment.spec.nodesPreinstalledApps }}
add {{ $app.url }} {{ $app.name }}-{{ trunc 10 (sha1sum $app.url) }}.jar
{{- end }}
