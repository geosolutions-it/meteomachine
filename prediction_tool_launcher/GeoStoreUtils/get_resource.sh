#!/bin/bash
#
# Delete a resource with the provided id
# 
FULL=${2:-"false"}

curl -u admin:admin  -XGET "http://localhost/geostore/rest/resources/resource/$1?full=$FULL"
