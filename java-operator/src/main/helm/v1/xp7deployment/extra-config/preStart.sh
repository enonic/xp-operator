#!/usr/bin/env bash

echo "Running prescript for node ${XP_NODE_NAME} (${XP_NODE_IP})"

echo "Bootstrap deploy folder apps"

# Delete apps
for f in $(find $XP_HOME/deploy -name '*.jar'); do
  name=$(basename $f)
  if [ "$(echo '{{ range $app := .Values.deployment.spec.nodesPreinstalledApps }}{{ $app.name }}-{{ trunc 10 (sha1sum $app.url) }}.jar{{ end }}' | grep $name)" == "" ]; then
    app.sh remove $name
  fi
done

# Add apps
{{- range $app := .Values.deployment.spec.nodesPreinstalledApps }}
app.sh add {{ $app.url }} --name={{ $app.name }}-{{ trunc 10 (sha1sum $app.url) }}.jar
{{- end }}

{{- if $.Values.deployment.clustered }}
bash {{ $.Values.dirs.extraConfig }}/dns.sh -d ${XP_NODE_NAME}.cluster-discovery.${NAMESPACE}.svc.cluster.local
{{- end }}
