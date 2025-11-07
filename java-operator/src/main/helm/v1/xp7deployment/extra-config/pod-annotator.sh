#!/bin/sh
set -e

NAMESPACE="$(cat /var/run/secrets/kubernetes.io/serviceaccount/namespace)"
POD_NAME="$(hostname)"
API="https://kubernetes.default.svc/api/v1/namespaces/${NAMESPACE}/pods/${POD_NAME}"

now() {
  date -u +"%Y-%m-%dT%H:%M:%SZ"
}

hash_config() {
  find "${XP_CONFIG_PATH}" -maxdepth 1 -type l 2>/dev/null \
    | grep -v "${XP_CONFIG_PATH}/\.\." \
    | xargs sha1sum 2>/dev/null \
    | awk '{print $1}' \
    | xargs echo
}

patch_annotation() {
  TIMESTAMP="$(now)"
  log "Detected config change, setting ${POD_CONFIG_RELOADED_ANNOTATION} = ${TIMESTAMP}"

  RESPONSE=$(curl -s -w "\n%{http_code}" -X PATCH "${API}" \
    -H "Authorization: Bearer $K8S_TOKEN" \
    -H "Content-Type: application/merge-patch+json" \
    --cacert /var/run/secrets/kubernetes.io/serviceaccount/ca.crt \
    -d "{\"metadata\": {\"annotations\": {\"${POD_CONFIG_RELOADED_ANNOTATION}\": \"${TIMESTAMP}\"}}}")

  BODY=$(echo "$RESPONSE" | head -n -1)
  CODE=$(echo "$RESPONSE" | tail -n1)

  if [ "$CODE" -ge 200 ] && [ "$CODE" -lt 300 ]; then
    log "Successfully patched pod annotation"
  else
    log "Failed to patch pod. HTTP $CODE. Response body:"
    echo "$BODY"
  fi
}

log() {
  echo "$(now) $@"
}

# Handle graceful shutdown
trap 'log "Received termination signal, exiting."; exit 0' TERM INT

LAST_HASH=""

log "Starting config reload watcher"
log "Watching: ${XP_CONFIG_PATH}, annotating: ${POD_CONFIG_RELOADED_ANNOTATION}"

while true; do
  CURRENT_HASH="$(hash_config)"
  if [ "$CURRENT_HASH" != "$LAST_HASH" ]; then
    LAST_HASH="$CURRENT_HASH"
    patch_annotation
  fi
  sleep 2
done
