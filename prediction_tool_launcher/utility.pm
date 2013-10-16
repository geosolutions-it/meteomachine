##############################################
# UTILITY
##############################################

###############################################
# applyIf(target,source)
# copy data if present from an hash to another (template)
###############################################
sub applyIf{
    my $target =$_[0];
    my $source = $_[1];
    foreach my $key ( keys %{$target} )
    {
        if( exists ($source->{$key})){
            $target->{$key}=$source->{$key};
        }
    }
    return $target;
}

#only for batch mode. call download script
sub downloadData{
    my $data=$_[0];
    my $dir= $_[1];
    my $tmp_dir_param= " -t " . $_[2] || "";
    my $glider=$data->{'gliderName'};
    
    my $experiment = $data->{'experimentName'};
    my $bathymetryFileName= $data->{"bathymetryFileName"};
    my $modelID=$data->{'modelID'};
    my $command = "./copy_ftp.sh -m $modelID -g $glider -c $experiment -d $dir  -b $bathymetryFileName $tmp_dir_param";
    return &executeCommand($command);
    
}
##############################################################################
# copyRunner(dir)
# copy the runner in the directory
##############################################################################
sub copyRunner{
    my $dir = $_[0];
    my $runnerDir = $_[1];
    &log("copying runner from $runnerDir into $dir");
    copy   ("$runnerDir"."run_gliderPathPrediction.sh","$dir"."run_gliderPathPrediction.sh");
    system("chmod a+x "."$dir"."run_gliderPathPrediction.sh");
    copy   ("$runnerDir"."gliderPathPrediction.ctf",$dir."gliderPathPrediction.ctf");
    copy   ("$runnerDir"."gliderPathPrediction",$dir."gliderPathPrediction");
    system("chmod a+x ".$dir."gliderPathPrediction");

    rcopy  ("$runnerDir"."img",$dir."img");
    dircopy("$runnerDir"."initializationScripts",$dir."initializationScripts");
    dircopy("$runnerDir"."inputDir",$dir."inputDir");
    system("rm ".$dir."outputDir/"."* -f");  #remove old generated kmz if present
}
sub copyInputDir{
        my $data = $_[0];
        my $dir = $_[1];
        my $path = $data->{'path'};
        &log("copy from $path to $dir");
        rcopy($path."/inputDir",$dir."inputDir");
}
##
# copyInitializationScripts from the dir in data->path to dir
##
sub copyInitializationScripts{
    my $data = $_[0];
    my $dir = $_[1];
    my $path = $data->{'path'};
    &log("copy from $path to $dir");
    rcopy($path."/initializationScripts",$dir."initializationScripts");

}
#
#readJsonFile(data,filename)

sub readJsonFile{
   my $data;
   my $filename = $_[0];
    if (open (my $json_stream, $filename)){
          local $/ = undef;
          my $json = JSON::PP->new;
          $data = $json->decode(<$json_stream>);
          close($json_stream);
          return $data;
        
    }else{
        #&close_error(21,"couldn't open file $filename");
        return ;
    }
}

#writeJsonFile(data,filename)
sub writeJsonFile{
    my $data=$_[0];
    my $filename=$_[1];
    open( my $fh, ">", $filename) or return 0;
    print $fh encode_json($data);
    close $fh;
    return 1;
}

sub jsonPrettyPrint{
    my $json =JSON->new->allow_nonref;
    return $json->pretty->encode($_[0]);
}

sub getJsonBaseDir{
    my $dir = $_[0] or $base_dir;
    return $dir . "initializationScripts/";
}

sub executeCommand{
    my $command = $_[0];
    &log("executingcommand\n: $command");
    system($command); 
    my $ret_code = ($? >> 8);
    &log("execution result code:$ret_code");
    return $ret_code;
    
}
sub close_success{
    &log("==============END=============");
    &log("PROGRAM FINISHED. message:$_[0]");
    exit 0;
}

sub close_error{
    print "\nERROR[" .  $_[0] .  "]:" . $_[1] . "\n";
    die "\nERROR[" .  $_[0] .  "]:" . $_[1] . "\n";
}

sub log{
    print "\nINFO:$_[0]\n" ;
}

1;