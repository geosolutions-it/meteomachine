use warnings;
use lib 'lib/';
use JSON;
use utility;


my $json1 = readJsonFile($ARGV[0]);
my $json2 = readJsonFile($ARGV[1]);


print &jsonPrettyPrint(&diff($json1,$json2));

sub diff{
    my $target =$_[0];
    my $source = $_[1];
    foreach my $key ( keys %{$target} )
    {
        if( exists ($source->{$key}) or exists ($target->{$key})){
                my $tarobj = $target->{$key} ;
                 my $souobj = $source->{$key} ;
                if( ! defined $tarobj){
                    $tarobj="null"
                }
                if( ! defined $souobj){
                    $souobj="null"
                }
                    
                
               
                $target->{$key}=  $tarobj . " --> " . $souobj;
            }
        
    }
    return $target;
}