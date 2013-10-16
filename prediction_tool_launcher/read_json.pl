#!/usr/bin/perl
#includes
use strict;
use lib 'lib/';
use JSON;
use HTTP::Request::Common qw(POST); 
use HTTP::Request::Common qw(GET);  
use LWP::UserAgent; 
use LWP::Simple;
use GeoStoreClient;
use File::Copy qw(copy);
use File::Copy::Recursive qw(fcopy rcopy dircopy fmove rmove dirmove);
use File::Find;
use File::Path 'rmtree';
use Cwd;
use utility;


my $mydir = getcwd;
################################################################################################
#
# This script provides to get a resource from GeoStore as a sort of runner configuration,
# parse this resource (json), copy the proper files, the prediction tool.
# It supports 2 operating modes ( using the onDemandRunningMode object in the resource)
# * On demand:
#       * create a disposible runner
#       * copy the directory from the path in the geostore resource
#       * cretes the json init files for the prediction tool
#       * run this instance and remove it
# * Batch: 
#       *(create a disposable runner)
#       * copy files using ftp 
#       * create the json init files for prediction tool
#       * Run the prediction tool
#
# For the actual settings you have to start the application with this options:
# perl read_json.pl <CRUISE_NAME> <batch|ondemand>
# this will search and iterate the uploaded resources in the batch|ondemand category 
# and parse them. After all (or maybe at the beginning, to avoid continuos faluires)
# if the resource contains onDemandRunningMode it will be deleted.
################################################################################################


# GLOBS
my $configuration = &readJsonFile('task_configuration.json');

#CRUISE NAME
my $cruiseName=$ARGV[0] || $configuration->{'cruiseName'};
my $categoryName=$ARGV[1] || $configuration->{'categoryName'};
my $base_dir=$ARGV[2] || $configuration->{'default_base_dir'};

#DESTINATION FILES AND DIRECTORY
my $json_files_folder = $configuration->{'json_files_folder'};
my $outputDir=$configuration->{'outputDir'};
my $mainfile_name=$configuration->{'mainfile_name'};
my $gliderFilePrefix=$configuration->{'gliderFilePrefix'};
my $runnerDir=$configuration->{'runnerDir'};
my $deployedMCRroot=$configuration->{'deployedMCRroot'};
my $temp_dir=$configuration->{'temp_dir'};
#TEMPLATES
my $templateDir = $configuration->{'templateDir'};
my $template_mainfile_name=$configuration->{'template_mainfile_name'};
my $template_gliderfile_name=$configuration->{'template_gliderfile_name'};
my $geostoreUrl = $configuration->{'geostoreUrl'};

#KMZ
my $kmz_regex_prefix = $configuration->{'kmz_regex_prefix'};
my $kmz_regex_suffix = $configuration->{'kmz_regex_suffix'};
my $kmz_dest_dir = $configuration->{'kmz_dest_dir'};

#OTHER
my $skipGliderParameters=$configuration->{'skipGliderParameters'};
my $gsUser=$configuration->{'gsUser'};
my $gsPassword=$configuration->{'gsPassword'};

#=====================================================================
######################################################################
########################## START PROGRAM #############################
######################################################################
#=====================================================================
&log("\n===========================\nstart execution at:".`date`."\n===========================\n");
#setup geostore client
my $gsClient = GeoStoreClient->new($geostoreUrl);
$gsClient->setBasicAuth($gsUser,$gsPassword);

#get resource list for the required category
my $resourcesList = $gsClient->getResourceList($categoryName);

#check errors getting resource list
if(not defined $resourcesList){
    close_error(23,"couldn t get resource List for $categoryName");
    
}
#check empty List
if ($resourcesList->{'ResourceList'} eq ""){
    &close_success("no resource to process");
}
#print &jsonPrettyPrint($resourcesList); #debug

##
# Creating an array for resources.
# NOTE: geostore returns an object instead of a resource if there
# is only one resource in the list. So we have to convert in an array
# with only one element ($resources[0])
##
my @resources=();
if(ref($resourcesList->{'ResourceList'}->{'Resource'}) eq 'HASH'){
    $resources[0]=$resourcesList->{'ResourceList'}->{'Resource'};
} else{
    &log( "found " . scalar(@resources) . " resource(s)");
    @resources=@{$resourcesList->{'ResourceList'}->{'Resource'}};
} 

##
# Cycle resources
##
foreach(@resources){
    my $resource =$_;
    #&log("TESTING RESOURCE: \n".&jsonPrettyPrint($resource));  #debug
    
    #resource name. e.g. NOMR12_elettra
    my $resourcename=$resource->{'name'};
    
    #resource id e.g. 34
    my $resourceId=$resource->{'id'};
    my $regex = "^".$cruiseName."_.*";
    
    #resource status, an attribute of the resource (pending,success, another message)
    my $status = &getStatusMessage($resourceId);
    
    if( defined($status) && defined($resourcename) && ($resourcename=~$regex) ){
        &log("matched resource $resourcename. Downloading ResourceData");
        my $data=$gsClient->getResourceData($categoryName,$resourcename);
        if(not defined $data){
            &setStatusMessage($resourceId,"couldn t get data for this resource");
            next;

        }
        #&log("RESOURCE: \n".&jsonPrettyPrint($data)); #debug
        &log("\n-------\nRunning resource: $resourcename\nid=$resourceId\nstatus:$status\n-------\n");
        
        #############
        # ON DEMAND #
        #############
        if($data->{'onDemandRunningMode'}>0){
            &log( "ONDEMAND\n");
            if($status != "Pending"){
                next;
            }
            # change status to executing to avoid multiple ondemand executions 
            &setStatusMessage($resourceId,"executing");
            # on demand requests must be deleted anyway, becouse can't be removed
            # elsewere
            
            my $dir = &createDisposableRunner($data,$resourcename);
            if ($dir eq ""){
                &setStatusMessage($resourceId,"unable to create runner");
                next;
            }
            &copyInputDir($data,$dir);
            &copyInitializationScripts($data,$dir);
            &writeInitializationScripts($data,$dir);
            my @generated_files = &runPredictionTool($data,$dir);
            $status = &copyKMZ($data,$dir); 
            if($status != "Success"){
                &log("---------RUNNER LOG------------------\n".&getRunnerLog($dir));
                &log("---------END OF RUNNER LOG-----------");
            }
            &setStatusMessage($resourceId,$status);
            &log( "Deleting resource (id:$resourceId) $resourcename");
            $gsClient->deleteResource($resourceId);    #comment for testing 
            &clearRunner($dir);

            
        #############
        #   BATCH   #
        #############
        }else{
            &log( "BATCH\n");
            #&setStatusMessage($resourceId,"Executing");
            
            my $dir = &createDisposableRunner($data,$resourcename);
            if ($dir eq ""){
                &setStatusMessage($resourceId,"unable to create runner");
                next;
            }
            my $retMess = &downloadData($data,$dir,$temp_dir);
            
            if( $retMess !=0){
                &setStatusMessage($resourceId,"error retriving data from FTP");
                &clearRunner($dir);
                next;
            }
            &writeInitializationScripts($data,$dir);
            my @generated_files = &runPredictionTool($data,$dir);
            $status = &copyKMZ($data,$dir);
            &log("\n------------------\nExecution Status:$status\n------------------\n");
            if($status != "Success"){
                &log("---------RUNNER LOG------------------\n".&getRunnerLog($dir));
                &log("---------END OF RUNNER LOG-----------");
            }
            &setStatusMessage($resourceId, $status);
            &clearRunner($dir);

        }
    }else{
        #my $list =  to_json($resourcesList);
        #my $json =JSON->new->allow_nonref;
        &log("Skipping this resource:\n-------\nId:$resourceId\nname:$resourcename\nstatus:$status\n------\n ");
    }
}
#TODO close success
&close_success("end reached");
#=====================================================================
######################################################################
########################## END   PROGRAM #############################
######################################################################
#=====================================================================
sub getStatusMessage{
    my $id= $_[0];
    return $gsClient->getAttribute($id,"statusMessage");
}
sub setStatusMessage{
    my $id= $_[0];
    my $mess= $_[1];
    return $gsClient->updateAttribute($id,"statusMessage",$mess);
}
##############################################################################
# writeInitializationScripts(data,dir)
# creates the initializationScripts from Data
##############################################################################
sub writeInitializationScripts{
    my $data = $_[0];
    my $dir = $_[1];
    &createInitFile($data,$dir); 
    &createGliderFile($data,$dir);

}
##############################################################################
# createInitFile(data,dir)
# creates the init file taking the name from adata or from the config file
##############################################################################
sub createInitFile{
    my $data=$_[0];
    my $dir = $_[1];

    my $template =&readJsonFile("$templateDir/$mainfile_name");
    &applyIf($template,$data);
    $template->{"onDemandRunningMode"} = 0;  #WORKAROUND for avoid errors
    if(exists $data->{'gliderName'}){
        $template->{'glider_names'}=$data->{'gliderName'}
    }
    my $scriptName=$data->{'gliderInputScriptName'} || $gliderFilePrefix . $data->{'gliderName'} . '.json';
    $data->{'gliderInputScriptName'} =$scriptName;
    my $mainfile = &getJsonBaseDir($dir) . $mainfile_name;
    &writeJsonFile($template,$mainfile);
    &log("written $mainfile");
    #&log(jsonPrettyPrint($template)); #DEBUG
}
##############################################################################
# createGliderFile(data,dir)
# creates the glider file taking the name from adata or from the config file
##############################################################################
sub createGliderFile{
    my $data=$_[0];
    my $dir = $_[1];

    my $template =&readJsonFile("$templateDir/$template_gliderfile_name");
    my $skip;
    foreach(@$skipGliderParameters){
        $skip->{$_}=$template->{$_}
    }
    &applyIf($template,$data);
    &applyIf($template,$skip);
    if(exists $data->{'gliderName'}){
        my $glider=$data->{'gliderName'};
        my $filename=$data->{'gliderInputScriptName'} || $gliderFilePrefix . $glider . '.json';
        my $json_base_dir=&getJsonBaseDir($dir);
        &writeJsonFile($template, getJsonBaseDir($dir).$filename);
        &log("written $json_base_dir$filename");
        #&log(jsonPrettyPrint($template)); #DEBUG
    }else{
        #&close_error(22,"Glider Name not present");
        return "Glider name not found";
    }
    return 1;
}
##############################################################################
# createDisposableRunner(data,resourcename)
#
# Creates a runner on the fly with the required files in the temp dir
# named as the resource name (e.g. REP10_onDemand_120)
##############################################################################
sub createDisposableRunner{
    my $data = $_[0];
    my $resourcename = $_[1];
    my $dir= "$temp_dir$resourcename/";
    unless(mkdir $dir) {
        #uncomment to force close if creation is not allowed
        #close_error(25, "Unable to create $resourcename\n");  
        #return "";
        
    }
    &copyRunner($dir,$runnerDir);
    return $dir;
}

sub runPredictionTool{
    my $data=$_[0];
    my $dir=$_[1];
    chdir ($dir);
    &executeCommand("./run_gliderPathPrediction.sh $deployedMCRroot $template_mainfile_name");
    my @files = glob $dir."outputDir/"."*.kmz";
    chdir ($mydir);
    return @files;
}

sub copyKMZ{
    my $data=$_[0];
    my $dir=$_[1];
    
    my $glidername = $data->{'gliderName'};
    my $regex = $kmz_regex_prefix.$glidername.$kmz_regex_suffix;
    opendir(DIR, $dir."outputDir/") or return "KML Not Generated. problems executing the runner";
    while (my $file = readdir(DIR)) {
        &log("$file matches the regex:$kmz_regex_prefix$glidername$kmz_regex_suffix"); 
        # Use a regular expression to find files ending in .txt
        next unless ($file =~ $regex);
        &log("copy $file to $kmz_dest_dir");
        closedir(DIR);
        my $sdir=$dir."outputDir/".$file;
        my $ddir= "$kmz_dest_dir$file";
        &log("copy from $sdir to $ddir");
        my $ret=copy($sdir,$ddir);
        if( $ret == 0 ){
            return "Unable to copy kmz:$!";
        }
        
        return "Success";
    }
    closedir(DIR);
    return "KML Not generated. problems executing runner";
    
}
sub getRunnerLog(){
    my $dir=$_[0];
    my $regex = "[0-9]{8}T[0-9]{4,6}Z?_logFile.txt";
    opendir(DIR, $dir."logs/") or return "KML Not Generated. problems executing the runner";
    while (my $file = readdir(DIR)) {
        my $content ="";
        open(my $fh, '<', $file) or return "cannot open file $file";
        {
            local $/;
            $content = <$fh>;
        }
        close($fh);
        return $content;
    }
    closedir(DIR);
    return "cannot open find runner Log;";
}
sub clearRunner{
    my $dir = $_[0];
    &log("removing directory $dir");
    rmtree($dir);
}

#sample

