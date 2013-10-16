#!/bin/bash
#
# Delete a resource with the provided id
# 

curl -u admin:admin  -XDELETE http://localhost/geostore/rest/resources/resource/$1
