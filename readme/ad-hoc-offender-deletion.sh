#!/bin/bash
# --------------------------------------------------------------------
# Ad Hoc Offender Deletion Event Trigger 
#
#     A script to request a single offender be referred
#     for deletion. The offender must be eligible for
#     deletion in line with the Ministry of Justice 
#     Records, Information Management and Retention Policy.
#     
#     In practice this means a Sentence End Date older than
#     7 years, inactive, out, non-lifer, single-booking,
#     no linked incidents, not UAL, no health problems,
#     no TACT alert, no IWP documents.
# 
#     The offender will then be checked for potential 
#     duplicates, manual retention, referral to Pathfinder,
#     or for certain offence codes or trigger words and may
#     be retained instead of being deleted.
# --------------------------------------------------------------------

VERSION=0.1.0
SUBJECT=ad-hoc-offender-deletion
USAGE="\n
Data Compliance Ad Hoc Offender Deletion\n
----------------------------------------\n\n
Request a single offender to be referred to the Data Compliance Service to be 
deleted.\n\n
Requirements: aws-cli, kubectl (with access to the MoJ live-1 cluster as dps-shared or dps-tech)\n\n
Usage: ./ad-hoc-offender-deletion.sh [-hv] <ENVIRONMENT> <OFFENDER_NO> \"<REASON>\"\n\n
ENVIRONMENT must be one of 'dev' or 'preprod'\n\n
Example: ./ad-hoc-offender-deletion.sh dev Z9999ZZ \"Deletion required for some reason\""

# --- Options processing -------------------------------------------
if [ $# == 0 ] ; then
    echo -e $USAGE
    exit 1;
fi

while getopts "vh" optname
  do
    case "$optname" in
      "v")
        echo "Version $VERSION"
        exit 0;
        ;;
      "h")
        echo -e $USAGE
        exit 0;
        ;;
      "?")
        echo "Unknown option $OPTARG"
        exit 0;
        ;;
    esac
  done

shift $(($OPTIND - 1))

ENVIRONMENT=$1
OFFENDER_NO=$2
REASON=$3

if ! [[ "$ENVIRONMENT" =~ ^(dev|preprod)$ ]]; then
    echo "ENVIRONMENT must be one of 'dev' or 'preprod'"
    exit 1;
fi

if ! [[ $REASON ]]; then
    echo -e $USAGE
    exit 1;
fi

# --- Locks -------------------------------------------------------
LOCK_FILE=/tmp/$SUBJECT.lock
if [ -f "$LOCK_FILE" ]; then
   echo "Script is already running"
   exit
fi

trap "rm -f $LOCK_FILE" EXIT
touch $LOCK_FILE

# --- Body --------------------------------------------------------
echo "Processing offender deletion request:"
echo "Environment: $ENVIRONMENT"
echo "Offender No: $OFFENDER_NO"
echo "Reason     : $REASON"

# Provide time to panic-kill process!
sleep 2

export AWS_ACCESS_KEY_ID=$(kubectl -n prison-data-compliance-$ENVIRONMENT get secrets data-compliance-response-sqs -o json | jq -r ".data | map_values(@base64d) | .access_key_id")
export AWS_SECRET_ACCESS_KEY=$(kubectl -n prison-data-compliance-$ENVIRONMENT get secrets data-compliance-response-sqs -o json | jq -r ".data | map_values(@base64d) | .secret_access_key")
QUEUE_URL=$(kubectl -n prison-data-compliance-$ENVIRONMENT get secrets data-compliance-response-sqs -o json | jq -r ".data | map_values(@base64d) | .sqs_dc_resp_url")

REASON_BODY="{\"offenderIdDisplay\":\"$OFFENDER_NO\",\"reason\":\"$REASON\"}"
REASON_ATTRIBUTES="eventType={StringValue=DATA_COMPLIANCE_AD-HOC-OFFENDER-DELETION,DataType=String}"

aws sqs send-message --queue-url $QUEUE_URL --message-body "$REASON_BODY" --message-attributes $REASON_ATTRIBUTES

echo "Deletion request sent!"
# -----------------------------------------------------------------
