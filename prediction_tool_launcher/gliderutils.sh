###################################################################
# log arg1
###################################################################
# wraps logging to proper redirect 
###################################################################
function log(){
    echo "INFO:" $1
}

###################################################################
# errormsg arg1
###################################################################
# writes error message to the log
###################################################################
function errormsg(){
    echo "ERROR[$1]:" ${EX_MESSAGE[$1]}
    if [ $# -eq 2 ]; then
        echo "DETAILS:$2" 
    fi
}

####################################################################
# close_program 
####################################################################
# close the program
####################################################################
function close_program(){
    exit $1

}

####################################################################
# close_error 
####################################################################
# send an error message and close the program
####################################################################
function close_error(){
    errormsg $1 $2
    close_program $1

}

###################################################################
# check_err orig_exit_code exitcode
###################################################################
# if the first number is not 0, exit with the second argument 
# leving a error message.
###################################################################
function check_err(){
    #echo $1; 
    if [ $1 -ne 0 ] ;
        then
            close_error $2
    fi
}

###################################################################
# lower arg1
###################################################################
# changes the case of the argument to lower case
###################################################################
function lower(){
    echo "$1" | tr '[:upper:]' '[:lower:]'
}

###################################################################
# upper arg1
###################################################################
# changes the case of the argument to upper case
###################################################################
function upper(){
     echo "$1" | tr '[:lower:]' '[:upper:]'
}

###################################################################
# download_single_file source_path destination path dest
###################################################################
# downloads a file from ftp passing path to the directory as 
# in second argument 
###################################################################
function download_single_file(){
    wget -np -o ${FTP_LOG} --directory-prefix ${TMP_DIR}/${INSTANCE_ID} --user ${USER} --password ${PASSWD} "ftp://${HOST}/${1}" 
    #TODO manage errors

}
###################################################################
# download_dir source_path destination path dest
###################################################################
# downloads a file from ftp passing path to the directory as 
# in second argument 
###################################################################
function download_dir(){
    log "downloading ${1} to ${TMP_DIR}/${INSTANCE_ID}/${HOST}"
    wget -m -np -o ${FTP_LOG} --directory-prefix ${TMP_DIR}/${INSTANCE_ID} --user ${USER} --password ${PASSWD} "ftp://${HOST}/${1}" 
    if [ $? -ne 0 ]
    then
        close_error $EX_DOWNLOAD_PROBLEM
    else
        log "ftp://${HOST}/${1} downloaded correctly" 
    fi

}

###################################################################
# inspect( host, port, username, password)
###################################################################
#
# Check if all the directory are present. check for some directory
# checking many types of case: 
# *as cruise name and glider name is
# *lower case
# *uppercase
# 
###################################################################
function inspect(){
    
    #generate upper and lower glider name
    lower_glider=$(lower ${GLIDER_NAME})
    upper_glider=$(upper ${GLIDER_NAME})
    
    #generate upper and lower cruise name
    upper_cruise=$(upper ${CRUISE_NAME})
    lower_cruise=$(lower ${CRUISE_NAME})
    GLIDERS_BASE=$upper_cruise/$GLIDERS_PATH
    MODEL_DIR_TEST=${upper_cruise}/${OCEAN_MOD_PATH}/${MODEL_ID}/
    current_model_file=${MODEL_LIST_FILE}_${INSTANCE_ID}
    current_glider_file=${GLIDER_LIST_FILE}_${INSTANCE_ID}
    current_archive_file=${ARCHIVE_LIST_FILE}_${INSTANCE_ID}
    
    ## First Connection is used to check right files and directories
    log "connecting  ${USER}@${HOST}:${PORT}"
    
    $FTP -in $HOST $PORT > ${FTP_LOG}  << EOF
user $USER $PASSWD

mls ${MODEL_DIR_TEST} ${current_model_file}
mls $GLIDERS_BASE${lower_glider}/ ${current_glider_file}
mls $GLIDERS_BASE${upper_glider}/ ${current_glider_file}
mls $GLIDERS_BASE${GLIDER_NAME}/ ${current_glider_file}
mls $ARCHIVE_BASE${CRUISE_NAME}/${upper_glider}/ ${current_archive_file}
mls $ARCHIVE_BASE${CRUISE_NAME}/${lower_glider}/ ${current_archive_file}
mls $ARCHIVE_BASE${CRUISE_NAME}/${GLIDER_NAME}/ ${current_archive_file}
mls $ARCHIVE_BASE${upper_cruise}/${upper_glider}/ ${current_archive_file}
mls $ARCHIVE_BASE${upper_cruise}/${lower_glider}/ ${current_archive_file}
mls $ARCHIVE_BASE${upper_cruise}/${GLIDER_NAME}/ ${current_archive_file}
mls $ARCHIVE_BASE${lower_cruise}/${upper_glider}/ ${current_archive_file}
mls $ARCHIVE_BASE${lower_cruise}/${lower_glider}/ ${current_archive_file}
mls $ARCHIVE_BASE${lower_cruise}/${GLIDER_NAME}/ ${current_archive_file}

bye

EOF
    ##################
    ## CHECK LAST RUN
    ##################
    cat $current_model_file
    cat $current_glider_file
    if [ -f ${current_model_file} ] 
    then
        log "looking for: ${MODEL_DIR_TEST}last_run"
        result_lr=$(grep -i "${MODEL_DIR_TEST}last_run$" ${current_model_file})
        tmp_res=$?
        
        if [ $tmp_res != "0" ]
            then
                log "last_run dir not found.try to find the latest dir."
                #The bigger "only number" named dir if last_run dir not here
                result=$(grep "${MODEL_DIR_TEST}[0-9]*$" ${current_model_file} | sort | tail -1)
                check_err $? $EX_LAST_RUN_DIR_NOT_FOUND
                LAST_RUN_DIR=$result_lr
            else
                LAST_RUN_DIR=$result_lr
        fi
        if [  "${LAST_RUN_DIR}" == "" ]
        then
            rm -f $current_model_file
            rm -f $current_glider_file
            rm -f $current_archive_file
            close_error $EX_LAST_RUN_DIR_NOT_FOUND
        fi

        LAST_RUN_DIR=$result_lr
    else
        rm -f $current_model_file
        rm -f $current_glider_file
        rm -f $current_archive_file
        close_error $EX_LAST_RUN_DIR_NOT_FOUND
    fi
    ######################
    ## CHECK GLIDER FOLDER
    ######################
    if [ -f ${current_glider_file} ] 
        then
            log "looking for: ${GLIDERS_BASE}${GLIDER_NAME}"
            result_gf=$(grep -i "${GLIDERS_BASE}${GLIDER_NAME}" ${current_glider_file} | sort | tail -1)
            check_err  $? $EX_GLIDER_DIR_NOT_FOUND
            GLIDER_DIR=$result_gf
            if [  "${GLIDER_DIR}" == "" ]
            then
                rm -f $current_model_file
                rm -f $current_glider_file
                rm -f $current_archive_file
                close_error $EX_GLIDER_DIR_NOT_FOUND "Glider:$GLIDER_NAME"
            fi
        else
            rm -f $current_model_file
            rm -f $current_glider_file
            rm -f $current_archive_file
           close_error $EX_GLIDER_DIR_NOT_FOUND "Glider:$GLIDER_NAME,$result_gf,${GLIDERS_BASE}${GLIDER_NAME}"
    fi
    #######################
    ## CHECK ARCHIVE FOLDER
    #######################
    if [ -f ${current_archive_file} ] 
        then
            log "looking for: $ARCHIVE_BASE${CRUISE_NAME}/${GLIDER_NAME}/archive"
            result_af=$(grep -i "$ARCHIVE_BASE${CRUISE_NAME}/${GLIDER_NAME}/archive$" ${current_archive_file} | sort | tail -1)
            check_err  $? $EX_ARCHIVE_DIR_NOT_FOUND
            ARCHIVE_DIR=$result_af
        else
           close_error $EX_ARCHIVE_DIR_NOT_FOUND 
         if [  "${ARCHIVE_DIR}" == "" ]
            then
                rm -f $current_model_file
                rm -f $current_glider_file
                rm -f $current_archive_file
                close_error $EX_LAST_RUN_DIR_NOT_FOUND
            fi
    fi
    
    rm -f $current_model_file
    rm -f $current_glider_file
    rm -f $current_archive_file

} 

###################################################################
# download_all skipmodel
###################################################################
#
# download all the directories locally
# if skipmodel don't copy again the model

# 
###################################################################
function download_all(){
    #remove old downloads
    #rm -rf ${TMP_DIR}/${INSTANCE_ID}/${HOST}
    echo "delete ${TMP_DIR}/${INSTANCE_ID}/${HOST}"
    download_dir ${GLIDER_DIR}
    if [[ $1 != 1 ]]; then
        download_dir ${LAST_RUN_DIR}
    fi
    download_dir ${ARCHIVE_DIR}
}
    
###################################################################
# move_all skipmodel
###################################################################
#
# moves downloaded dirs in.
# if skipmodel don't copy again the model
# 
###################################################################
function move_all(){
    #remove old downloads
    $(mkdir -p ${GLIDER_DEST}/$GLIDER_NAME)
    check_err $? $EX_COPY_ERROR
    cp -rf $TMP_DIR/${INSTANCE_ID}/${HOST}/${GLIDER_DIR}/matfiles/* ${GLIDER_DEST}/$GLIDER_NAME
    cp -rf $TMP_DIR/${INSTANCE_ID}/${HOST}/${GLIDER_DIR}/parsed/* ${GLIDER_DEST}/$GLIDER_NAME
    check_err $? $EX_COPY_ERROR
    log "copied files in ${GLIDER_DEST}/$GLIDER_NAME from ${HOST}/${GLIDER_DIR}/matfiles and ${HOST}/${GLIDER_DIR}/parsed ";
    if [[ $1 != 1 ]]; then
        $(mkdir -p $LAST_RUN_DEST)
        check_err $? $EX_COPY_ERROR
        cp -rf $TMP_DIR/${INSTANCE_ID}/${HOST}/${LAST_RUN_DIR}/* $LAST_RUN_DEST
        check_err $? $EX_COPY_ERROR
        log "copied flies in $LAST_RUN_DEST from $LAST_RUN_DIR";
    fi
    $(mkdir -p $ARCHIVE_DEST/$GLIDER_NAME)
    check_err $? $EX_COPY_ERROR
    cp  -rf $TMP_DIR/${INSTANCE_ID}/${HOST}/${ARCHIVE_DIR}/* $ARCHIVE_DEST/$GLIDER_NAME
    
    log "copied flies in $ARCHIVE_DEST/$GLIDER_NAME";
    check_err $? $EX_COPY_ERROR
    
    
}

function clear_ftp_cache(){
    rm -rf ${TMP_DIR}/${INSTANCE_ID}
    echo "delete ${TMP_DIR}/${INSTANCE_ID}/${HOST}"
}
