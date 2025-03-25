#!/usr/bin/env bash

set -e

usage () {
    echo "Usage: $SCRIPT_NAME [OPTIONS]"
    echo "Wait until IP has been added to endpoints"
    echo ""
    echo "Options:"
    echo "  -s, --service=     Service name"
    echo "  -i, --ip=          IP address"
    echo "  -t, --timeout=     Script timeout"
    echo "  -a, --additional=  Additional seconds to wait at end"
    echo "  -r, --reverse      Wait until endpoint is gone"
    echo "  -h, --help         Print usage"
    exit 1
}

SERVICE=""
IP=""
TIMEOUT="20"
ADDITIONAL_WAIT_SECONDS="0"
REVERSE="0"

while [ "$#" -gt 0 ]; do
    case $1 in
        -h | --help)
            usage ;;
        -s)
            shift; SERVICE="$1" ;;
        --service=*)
            SERVICE=$(echo $1 | awk '{split($0,r,"="); print r[2]}') ;;
        -i)
            shift; IP="$1" ;;
        --ip=*)
            IP=$(echo $1 | awk '{split($0,r,"="); print r[2]}') ;;
        -t)
            shift; TIMEOUT="$1" ;;
        --timeout=*)
            STATE=$(echo $1 | awk '{split($0,r,"="); print r[2]}') ;;
        -a)
            shift; ADDITIONAL_WAIT_SECONDS="$1" ;;
        --additional=*)
            ADDITIONAL_WAIT_SECONDS=$(echo $1 | awk '{split($0,r,"="); print r[2]}') ;;
        -r | --reverse)
            REVERSE="1" ;;
        *)
            usage
            ;;
    esac
    shift
done

if [ "${SERVICE}" == "" ]; then
    echo "Missing service parameter!"
    usage
fi

if [ "${IP}" == "" ]; then
    echo "Missing ip parameter!"
    usage
fi

if [ "$REVERSE" = "0" ]; then
    echo -n "Waiting for endpoint ${IP} to appear in service ${SERVICE}: "
else
    echo -n "Waiting for endpoint ${IP} to be removed in service ${SERVICE}: "
fi

echo "${SERVICE} [timeout: ${TIMEOUT}, additional: ${ADDITIONAL_WAIT_SECONDS}]"

OK="0"
FOUND="0"

START_TIME=$(date +%s)
while [ "$OK" == "0" ]; do
    (curl -s -H "Authorization: Bearer `cat /var/run/secrets/kubernetes.io/serviceaccount/token`" --cacert /var/run/secrets/kubernetes.io/serviceaccount/ca.crt https://kubernetes.default.svc.cluster.local/api/v1/namespaces/`cat /var/run/secrets/kubernetes.io/serviceaccount/namespace`/endpoints/${SERVICE} | grep ${IP} > /dev/null) && FOUND="1" || true

    if [ "$FOUND" == "1" ]; then
        echo "IP ${IP} in service ${SERVICE} found!"
        if [ "${REVERSE}" == "0" ]; then
            OK="1"
        fi
    else
        echo "IP ${IP} in service ${SERVICE} not found!"
        if [ "${REVERSE}" == "1" ]; then
            OK="1"
        fi
    fi

    TIME=`printf "%s\n" $(( $(date +%s) - ${START_TIME} ))`
    if [ "$TIME" -gt "$TIMEOUT" ]; then
        echo "Operation timed out!"
        break
    fi

    if [ "$OK" == "0" ]; then
        sleep 2
    fi
done

if [ "${ADDITIONAL_WAIT_SECONDS}" != "0" ]; then
    echo "Waiting for additional ${ADDITIONAL_WAIT_SECONDS} seconds"
    sleep ${ADDITIONAL_WAIT_SECONDS}
fi

if [ "${OK}" != "1" ]; then
    exit 1
fi
