echo "Running prescript for node ${XP_NODE_NAME} (${XP_NODE_IP})"

echo "Bootstrap deploy folder apps"

# Delete apps
for f in $(find $XP_HOME/deploy -name '*.jar'); do
  name=$(basename $f)
  if [ "$(echo '{{ range $k, $app := .Values.preInstalledApps }}{{ $k }}.jar{{ end }}' | grep $name)" == "" ]; then
    app.sh remove $name
  fi
done

# Add apps
{{- range $k, $app := .Values.preInstalledApps }}
app.sh add {{ $app }} --name={{ $k }}.jar
{{- end }}

{{- if $.Values.deployment.clustered }}
bash {{ $.Values.dirs.extraConfig }}/dns.sh -d cluster-discovery.${NAMESPACE}.svc.cluster.local
bash {{ $.Values.dirs.extraConfig }}/dns.sh -d ${XP_NODE_NAME}.cluster-discovery.${NAMESPACE}.svc.cluster.local
{{- end }}