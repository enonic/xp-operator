#!/bin/bash

minikube start \
    --cpus=4 \
    --memory=16gb \
    --disk-size=20gb \
    --kubernetes-version=v1.15.8 \
    --vm-driver=virtualbox

kubectl config use-context minikube

minikube addons enable ingress

# Add linkerd sidecar to ingress, skip proxying inbound ports
kubectl -n kube-system patch deployment nginx-ingress-controller --patch "$(cat src/test/minikube/patches/ingress-annotation-patch.yaml)"

# Enable caching on ingress controller
kubectl -n kube-system patch configmap nginx-load-balancer-conf --patch "$(cat src/test/minikube/patches/ingress-config-snippet-patch.yaml)"

# Restart the pod
kubectl -n kube-system delete pod "$(kubectl -n kube-system get pods --selector=app.kubernetes.io/name=nginx-ingress-controller -o jsonpath='{.items[0].metadata.name}')"