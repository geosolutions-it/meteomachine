#!/bin/bash
#
# add a resource with the provided file
# 

curl -u admin:admin  -XPOST -T $1 http://localhost/geostore/rest/resources
