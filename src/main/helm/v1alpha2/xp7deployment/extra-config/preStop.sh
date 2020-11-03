echo "Wait for endpoints to be deleted"
bash {{ $.Values.dirs.extraConfig }}/endpoints.sh -r -s {{ $.Values.allNodesKey }} -i ${XP_NODE_IP} -t 10
bash {{ $.Values.dirs.extraConfig }}/endpoints.sh -r -s ${XP_NODE_GROUP} -i ${XP_NODE_IP} -t 10 -a 20