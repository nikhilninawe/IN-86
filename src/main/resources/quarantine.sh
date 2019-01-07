#!/bin/bash

# detach.sh i-000123456789

# deregisters a machine from its load balancer and detaches the
# machine from its auto-scaling group. once RCA is complete,
# the machine can be terminated using terminate.sh

###########################################################
# setup
###########################################################

# source project's helper functions
source helpers.sh

# how should this script be invoked?
[[ -z $1 ]] && error "$0 {machine}" && exit 1

# get machine id from input arguments
machine=$1


###########################################################
# main
###########################################################

# start the timer
SECONDS=0

# write header to console
header "$(basename $0)"

# exit if operators negates prompt
#prompt "Detach $machine?"

###########################################################
# deregister
###########################################################

# get load balancer name from aws
proxy=$(
  aws elb describe-load-balancers \
    --query "LoadBalancerDescriptions[?Instances[?InstanceId=='${machine}']].LoadBalancerName" \
    --output text
)

# if load balancer found, deregister machine
if [ ! -z "$proxy" ]; then

  # remove instance from LB
  aws elb deregister-instances-from-load-balancer \
    --load-balancer-name $proxy \
    --instances $machine

  # wait for instance to deregister
  aws elb wait instance-deregistered \
    --load-balancer-name $proxy \
    --instances $machine

  # write to console
  info "Deregistered ${machine} from ${proxy}"

fi

###########################################################
# detach
###########################################################

# get auto-scaling group name from aws
cluster=$(
  aws autoscaling describe-auto-scaling-instances --instance-ids="${machine}" \
    --query "AutoScalingInstances[].AutoScalingGroupName" \
    --output text
)

# if cluster found, detach machine
if [ ! -z "$cluster" ]; then

  # detach machine from auto-scaling group
  aws autoscaling detach-instances \
    --instance-ids $machine \
    --auto-scaling-group-name $cluster \
    --no-should-decrement-desired-capacity

  # write to console
  info "Detached ${machine} from ${cluster}"

fi

###########################################################
# exit
###########################################################

# print total elapsed time to console
info "Finished $(basename $0) in ${SECONDS} seconds"

# if you got here, exit clean
exit 0
