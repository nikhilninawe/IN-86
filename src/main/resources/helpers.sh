#!/bin/bash

# source helpers.sh

# a collection of helper functions for logging, validating, etc.
# source by all solo scripts

###########################################################
# header
###########################################################

# print turvo in large text followed by script name
# usage: header
function header {
  [[ -f "turvo.txt" ]] && cat turvo.txt && echo
  echo "> compute/$1"
}

###########################################################
# prompt
###########################################################

# prompt operator for confirmation [Y/n]
# usage: header
function prompt {
  prompt=$(info "$1 Are your sure? [Y/n] ")
  read -p "$prompt" -n 1 -r && echo
  [[ ! $REPLY =~ ^[Yy]$ ]] && exit 1
}


###########################################################
# logging
###########################################################

# print a timestamped message to standard out
# usage: log {severity} {message}
# example: 2017-12-14 09:56:17 INFO I like to hokey pokey
function log {
  echo -e "$(date '+%Y-%m-%d %H:%M:%S') $1 $2"
}

# print INFO message to standard out
# usage: info {message}
# example: 2017-12-14 09:56:17 INFO I really do!
function info {
  log "INFO" "$1"
}

# print ERROR message to standard out
# usage: error {message}
# example: 2017-12-14 09:56:17 ERROR Oops! What?!?
function error {
  log "ERROR" "$1"
}

# print ERROR and exit with non-zero return code
# usage: die {message}
function die {
  error "$1" && exit 1
}

###########################################################
# templating
###########################################################

# subsitutes placedholers in a file with variables
# usage: FOO="bar" templatize {file}
function templatize {
  eval "echo \"$(cat $1)\""
}

###########################################################
# validation
###########################################################

# must be a valid file or die
# usage: must_be_a_file {path}
function must_be_a_file {
  [[ -f "$1" ]] || die "$1 not found"
}

# must be a valid directory or die
# usage: must_be_a_directory {path}
function must_be_a_directory {
  [[ -d "$1" ]] || die "$1 not found"
}

# must be a valid link or die
# usage: must_be_a_link {path}
function must_be_a_link {
  [[ -e "$1" ]] || die "$1 not found"
}

# must be a set variable or die
# usage: must_be_a_link {variable}
function must_be_a_variable {
  [[ -z "${!1}" ]] && die "$1 not set"
}

###########################################################
# syntax
###########################################################

# file must contain valid json or die
# usage: must_be_valid_json_syntax {path}
function must_be_valid_json_syntax {
  jq . $1 &>/dev/null || die "$1 not valid json"
}

# string must not contain slashes or die
# usage: must_not_contain_slashes {string}
function must_not_contain_slashes {
  if [[ "$1" == *\/* ]] || [[ "$1" == *\\* ]] ; then
    die "$1 has slashes"
  fi
}

###########################################################
# aws
###########################################################

# aws s3 object must exists or die
# usage: must_be_s3_object s3://{path}
function must_be_an_s3_object {
  aws s3 ls $1 > /dev/null
  if [ $? -ne 0 ] ; then
  	die "$1 not found"
  fi
}

# aws s3 object must exists or die
# usage: must_not_be_s3_object s3://{path}
function must_not_be_an_s3_object {
  aws s3 ls $1
  if [ $? -eq 0 ] ; then
  	die "$1 already exists"
  fi
}

# aws cloudformation syntax must be valid or die
# usage: must_be_valid_cloudformation_syntax {path}
function must_be_valid_cloudformation_syntax {
  aws cloudformation validate-template --template-body file://$1 &>/dev/null
  if [ $? -ne 0 ] ; then
    die "$1 not valid cloudformation syntax"
  fi
}

# aws cloudformation stack must not exist or die
# usage: must_not_be_cloudformation_stack {name}
function must_not_be_a_cloudformation_stack {
  aws cloudformation describe-stacks --stack-name $1 &>/dev/null
  if [ $? -eq 0 ] ; then
    die "$1 already exists"
  fi
}

# create aws cloudformation stack
# usage: aws_create_stack {name} {template} {parameters}
function aws_create_stack {
  aws cloudformation create-stack --stack-name $1 \
    --template-body file://$2 \
    --parameters file://$3 \
    --capabilities CAPABILITY_IAM \
    --disable-rollback \
    --output text
}

# update aws route 53 record
# usage: aws_route {zone} {source} {target}
function router {
  zone=$1 ; record=$2 ; target=$3
  info "Routing $record to $target"
  aws route53 change-resource-record-sets \
    --hosted-zone-id $zone \
    --change-batch '{ "Changes": [{ "Action": "UPSERT", "ResourceRecordSet": { "Type": "CNAME", "Name": "'"$record"'", "ResourceRecords": [{ "Value": "'"$target"'" }], "TTL": 60 }}]}' \
    --output text &>/dev/null
}

###########################################################
# slack
###########################################################

# send a notification via Slack
# usage: slack {message}
function slack {
  webhook="https://hooks.slack.com/services/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"
  curl -s -XPOST --data-urlencode "payload={ \"text\" : \"${1}\", \"channel\": \"#compute\" }" $webhook &>/dev/null
}
