#!/bin/bash
#
# Update a resource with the provided id
# 

curl -u admin:admin  -XPOST -T $2 http://localhost/geostore/rest/resources/resource/$1
