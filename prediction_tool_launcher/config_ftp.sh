##############################################################################
## BASE PARAMS ##
#################
#defaults
DEFAULTHOST="192.168.1.70"
DEFAULTPORT=21
DEFAULTUSER="admin"
DEFAULTPW='Crociera100!'

#ftp connection parameters
HOST=$DEFAULTHOST
PORT=$DEFAULTPORT
USER=$DEFAULTUSER
PASSWD=$DEFAULTPW

log "FTP host: $HOST"

##############################################################################
## TEMP FILES ##
################
TMP_DIR=tmp_files_dir
FTP_LOG=$TMP_DIR/ftp_log.log
MODEL_LIST_FILE=$TMP_DIR/modelList
GLIDER_LIST_FILE=$TMP_DIR/gliderList
ARCHIVE_LIST_FILE=$TMP_DIR/archiveList
#path for <CRUISE><OCEAN_MOD_PATH><MODELNAME>
# e.g REP12/data/NURC/oceanmod/Ekf12
OCEAN_MOD_PATH=data/NURC/oceanmod

############################################################################## 
# EXCHANGE /GLOB VARS ##
########################
#remote base directory
GLIDERS_PATH=data/NURC/gliders/
ARCHIVE_BASE=GLIDERRES/data/NURC/dss/
#subfolders folders parsed and matfiles
log "Temp Dir: $TMP_DIR"
