#!/bin/sh
set -e

now() {
  date -u +"%Y-%m-%dT%H:%M:%SZ"
}

randomId() {
  cat /dev/urandom | tr -dc 'a-z0-9' | fold -w 16 | head -n 1
}

log() {
  echo -n "$(now) $@"
}

logWithNL() {
    log "$@"
    echo ""
}

RUNNING="true"

finish() {
    logWithNL "Stopping event loop"
    RUNNING="false"
}
trap finish TERM INT

buildEventConfigMapChange() {
    cat << EOF
{
    "apiVersion": "v1",
    "involvedObject": {
        "apiVersion": "v1",
        "kind": "Pod",
        "name": "${XP_NODE_NAME}",
        "namespace": "${NAMESPACE}",
        "fieldPath": "spec.containers{exp}",
        "uid": "${POD_UID}"
    },
    "kind": "Event",
    "lastTimestamp": "$(now)",
    "firstTimestamp": "$(now)",
    "message": "Pod ${XP_NODE_NAME} reloaded ConfigMap ${XP_NODE_GROUP}",
    "metadata": {
        "name": "${XP_NODE_NAME}.$(randomId)",
        "namespace": "${NAMESPACE}"
    },
    "reason": "ConfigReload",
    "related": {
        "apiVersion": "v1",
        "kind": "ConfigMap",
        "name": "${XP_NODE_GROUP}",
        "namespace": "${NAMESPACE}"
    },
    "source": {
      "component": "pod/${XP_NODE_NAME}"
    },
    "type": "Normal"
}
EOF
}

sendEvent() {
  (cat - | curl -f -s -S -X POST \
    -H "Accept: application/json" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $K8S_TOKEN" \
    --cacert /var/run/secrets/kubernetes.io/serviceaccount/ca.crt \
    -d @- \
    "https://kubernetes.default.svc.cluster.local/api/v1/namespaces/$(cat /var/run/secrets/kubernetes.io/serviceaccount/namespace)/events" \
    > /dev/null) || (echo "failed:" >&2 && false) && echo "success"
}

cmEvent() {
  log "Sending ConfigReload event ... "
  buildEventConfigMapChange | sendEvent
}

getConfigHash() {
  find "${XP_CONFIG_PATH}/" -maxdepth 1 -type l | grep -v "${XP_CONFIG_PATH}/\.\." | xargs sha1sum | awk '{print $1}' | xargs echo
}

OLD_HASHCODE=""

logWithNL "Starting event loop"

while [ "$RUNNING" = "true" ]; do
  NEW_HASHCODE=$(getConfigHash)
  if [ "$NEW_HASHCODE" != "$OLD_HASHCODE" ]; then
    cmEvent
  fi
  OLD_HASHCODE=$NEW_HASHCODE
  sleep 2
done
