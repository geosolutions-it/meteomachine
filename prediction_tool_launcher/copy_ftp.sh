#!/bin/bash

##############################################################################
# this script allows to copy the proper files for a gliders prediction tool
# ingestion flow
##############################################################################
# PROGRAMS
FTP=/usr/bin/ftp
##############################################################################
##   INCLUDES   ##
##################
. ./gliderutils.sh
log "inizializing configuration"
. ./config_ftp.sh
##############################################################################
## CURRENT PARAMETERS ## 
########################
#Model 
MODEL_ID=
#Bathymetry file name
BATHYMETRYFILENAME=
#the name of the glider
GLIDER_NAME=
#the name of the cruise
CRUISE_NAME=

#boolean skip model download 
SKIPMODELDOWNLOAD=
## INSTANCE_ID ## 
INSTANCE_ID=$(date +%s.%N);
GLIDERS_BASE=
############################################################################## 
# DESTINATION DIRS    ##
########################
DEST_BASE=dest_test
GLIDER_REL_PATH=inputDir/gliderParameters
LAST_RUN_REL_PATH=inputDir/models
ARCHIVE_REL_PATH=inputDir/gliderParameters
GLIDER_DEST=$DEST_BASE$GLIDER_REL_PATH
LAST_RUN_DEST=$DEST_BASE$LAST_RUN_REL_PATH
ARCHIVE_DEST=$DEST_BASE$ARCHIVE_REL_PATH
##############################################################################
##  EXCEPTIONS ##
#################

##ERROR CODES
EX_LAST_RUN_DIR_NOT_FOUND=10
EX_GLIDER_DIR_NOT_FOUND=11
EX_ARCHIVE_DIR_NOT_FOUND=12
EX_DOWNLOAD_PROBLEM=13
EX_BAD_USAGE=14

##ERROR MESSAGES
EX_MESSAGE[10]="Couldn't find Last Run directory"
EX_MESSAGE[11]="Gliders dir not found"
EX_MESSAGE[12]="Archive dir not found"
EX_MESSAGE[13]="DOWNLOAD PROBLEM"
EX_MESSAGE[14]="Bad script usage"
##############################################################################
##  VARIABLES  ##
#################
GLIDER_DIR=
#archive subdir
LAST_RUN_DIR=
ARCHIVE_DIR=
## UTILITY METHODS
function usage(){
    echo "Usage: -h host -p port -U username -P password -m modelid -b bathimetryfilename -c cruisename -g glidername -d base_dir (-s)"

}
##############################################################################
##############################################################################
## PROGRAM START #############################################################
##############################################################################
##############################################################################

########################
##   READ ARGUMENTS   ##
########################
while getopts :h:p:U:P:m:d:t:b:g:c:s OPTION
do
 	case $OPTION in
     	h)
         	HOST=$OPTARG
         	;;
     	p)
         	PORT=$OPTARG
         	;;
     	U)
         	USER=$OPTARG
         	;;
     	P)
         	PASSWD=$OPTARG
         	;;
     	m)
         	MODEL_ID=$OPTARG
         	;;
     	b)
         	BATHYMETRYFILENAME=$OPTARG
         	;;
     	c)
         	CRUISE_NAME=$OPTARG
         	;;
     	g)
         	GLIDER_NAME=$OPTARG
         	;;
        s) SKIPMODELDOWNLOAD=1
            ;;
        d) 
            DEST_BASE=$OPTARG  
            GLIDER_DEST=$DEST_BASE$GLIDER_REL_PATH
            LAST_RUN_DEST=$DEST_BASE$LAST_RUN_REL_PATH
            ARCHIVE_DEST=$DEST_BASE$ARCHIVE_REL_PATH
            ;;
         t)
            TMP_DIR=$OPTARG
            FTP_LOG=$TMP_DIR/ftp_log.log
            MODEL_LIST_FILE=$TMP_DIR/modelList
            GLIDER_LIST_FILE=$TMP_DIR/gliderList
            ARCHIVE_LIST_FILE=$TMP_DIR/archiveList 
            ;;
     	?)
         	usage
         	error_exit $EX_BAD_USAGE
         	;;
 	esac
done
#TODO check if all parameters are present
#create template dir if not present
[ -d $TMP_DIR ] || mkdir $TMP_DIR
log "Temporaney Directiory ${TMP_DIR}"
#list FTP in order to get the latest directories
inspect
log "Glider Dir  : ${GLIDER_DIR}"
log "Last Run Dir: ${LAST_RUN_DIR}"
log "Archive Dir: ${ARCHIVE_DIR}"
#download dirs locally
download_all $SKIPMODELDOWNLOAD
#copy downloaded files to the destination dir 
move_all $SKIPMODELDOWNLOAD
#remove downloaded files
clear_ftp_cache 
#clear_tmp



