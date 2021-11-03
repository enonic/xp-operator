#!/usr/bin/env bash

set -e

usage () {
    echo "Usage: $SCRIPT_NAME [OPTIONS]"
    echo "Wait until DNS record is present"
    echo ""
    echo "Options:"
    echo "  -d, --dns=         Record to poll"
    echo "  -t, --timeout=     Script timeout"
    echo "  -a, --additional=  Additional seconds to wait at end"
    echo "  -r, --reverse      Wait until record is gone"
    echo "  -h, --help         Print usage"
    exit 1
}

DNS=""
TIMEOUT="60"
ADDITIONAL_WAIT_SECONDS="0"
REVERSE="0"

while [ "$#" -gt 0 ]; do
    case $1 in
        -h | --help)
            usage ;;
        -d)
            shift; DNS="$1" ;;
        --dns=*)
            DNS=$(echo $1 | awk '{split($0,r,"="); print r[2]}') ;;
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

if [ "${DNS}" == "" ]; then
    echo "Missing dns parameter!"
    usage
fi

if [ "$REVERSE" = "0" ]; then
    echo -n "Waiting for DNS record to appear: "
else
    echo -n "Waiting for DNS record to be removed: "
fi

echo "${DNS} [timeout: ${TIMEOUT}, additional: ${ADDITIONAL_WAIT_SECONDS}]"

OK="0"
FOUND="0"

START_TIME=$(date +%s)
while [ "$OK" == "0" ]; do
    (dig +short ${DNS} | grep -v -e '^$' > /dev/null) && FOUND="1" || true

    if [ "$FOUND" == "1" ]; then
        echo "Record ${DNS} found!"
        if [ "${REVERSE}" == "0" ]; then
            OK="1"
        fi
    else
        echo "Record ${DNS} not found!"
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