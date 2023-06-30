#!/usr/bin/env bash

echo "Running prescript for node ${XP_NODE_NAME} (${XP_NODE_IP})"

{{- if $.Values.deployment.clustered }}
bash {{ $.Values.dirs.extraConfig }}/dns.sh -d ${XP_NODE_NAME}.cluster-discovery.${NAMESPACE}.svc.cluster.local
{{- end }}
