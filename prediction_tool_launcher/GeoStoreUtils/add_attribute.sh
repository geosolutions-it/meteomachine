#!/bin/bash
#
# Update a resource with the provided id
# 

curl -u admin:admin  -XPUT  http://localhost/geostore/rest/resources/resource/$1/attributes/$3/$4
