package GeoStoreClient;
use LWP::UserAgent; 
use LWP::Simple;
use lib 'lib/';
use JSON;
use LWP::UserAgent;
use HTTP::Request::Common qw(POST); 
use HTTP::Request::Common qw(GET); 
use MIME::Base64;
##############################################
# Usage: 
# my $gsClient = GeoStoreClient->new($geostoreUrl);
# $gsClient->setBasicAuth("user","password");
# my $resourcesList = $gsClient->getResourceList($categoryName);
# my $resource = $gsClient->getResourceData("category","name");
##############################################
sub new{
	my $class = shift;
    my $self = {
        _url => shift
    };
	bless $self, $class;
    return $self;
}

sub getResourceList{
	my $self = shift;
	my $ua = LWP::UserAgent->new;
	#get resource list
	my $url =$self->{_url}.'/rest/misc/category/name/'.$_[0].'/resources';
	my $req = HTTP::Request->new(GET => $url);
	$req->header($self->getHeaders()); 
	my $res = $ua->request($req);
	# check the outcome
	if ($res->is_success) {
		my $json =JSON::PP->new;
		return  $json->decode($res->decoded_content);
	}
	else {
        print STDERR "\nerror request: $url";
        print STDERR "\nresponse:".$res->status_line( );
        print STDERR "\ncontent:".$res->content( );
		return;
	} #TODO ERROR management
}

sub getResourceData{
    my $self = shift;
	my $ua = LWP::UserAgent->new;
	#get resource list
    #/misc/category/name/{cname}/resource/name/{rname}/data GET
    
	my $url =$self->{_url}.'/rest/misc/category/name/'.$_[0].'/resource/name/'.$_[1].'/data';
	
	my $req = HTTP::Request->new(GET => $url);
	$req->header($self->getHeaders()); #doesn't seems to work
    #print $req->headers_as_string; #DEBUG
	my $res = $ua->request($req);
	# check the outcome
	if ($res->is_success) {
		my $json =JSON::PP->new;
		return  $json->decode($res->decoded_content);
	}
	else {
        print STDERR "\nerror request: $url";
        print STDERR "\nresponse:".$res->status_line( );
        print STDERR "\ncontent:".$res->content( );
		return;
	} #TODO ERROR management
}
sub getResource{
    my $self = shift;
	my $ua = LWP::UserAgent->new;
	#get resource list
    #/misc/category/name/{cname}/resource/name/{rname}/data GET
    
	my $url =$self->{_url}.'/rest/resources/resource/'.$_[0]; 
	
	my $req = HTTP::Request->new(GET => $url);
	$req->header($self->getHeaders()); #doesn't seems to work
    #print $req->headers_as_string; #DEBUG
	my $res = $ua->request($req);
	# check the outcome
	if ($res->is_success) {
		my $json =JSON::PP->new;
		return  $json->decode($res->decoded_content);
	}
	else {
        print STDERR "\nerror request: $url";
        print STDERR "\nresponse:".$res->status_line( );
        print STDERR "\ncontent:".$res->content( );
		return;
	}
}
#
# add or update an attribute of a resource. It dosn't run because
# geostore dosn't support json body with HTTP PUT for resources.
#
sub updateAttribute{
    my $self = shift;
	my $ua = LWP::UserAgent->new;
	#get resource list
    #/misc/category/name/{cname}/resource/name/{rname}/data GET
    my $resourceId =$_[0];
    my $attname = $_[1];
    my $attvalue = $_[2];
    $resource->{'Resource'}->{'Attributes'}->{'attribute'} = \@Attrs;
	my $url =$self->{_url}.'/rest/resources/resource/'.$resourceId . '/attributes/'. $attname . '/'. $attvalue; 
	my $req = HTTP::Request->new(PUT => $url);
	$req->header($self->getHeaders()); #doesn't seems to work
    $req->authorization_basic($self->{_user}, $self->{_password});   
	my $res = $ua->request($req);
	# check the outcome
	if ($res->is_success) {
		return  $resource;
	}
	else {
        print STDERR "\nerror request: $url";
        print STDERR "\nresponse:".$res->status_line( );
        print STDERR "\ncontent:".$res->content( );
		return;
	} 
}
sub getAttribute{
    my $self = shift;
	my $ua = LWP::UserAgent->new;
	#get resource list
    #/misc/category/name/{cname}/resource/name/{rname}/data GET
    my $resourceId =$_[0];
    my $attname = $_[1];
    my $attvalue = $_[2];
	my $url =$self->{_url}.'/rest/resources/resource/'.$resourceId. '/attributes/'. $attname ;
	my $req = HTTP::Request->new(GET => $url);
	$req->header($self->getHeaders()); #doesn't seems to work
    $req->authorization_basic($self->{_user}, $self->{_password});   
	my $res = $ua->request($req);
	# check the outcome
	if ($res->is_success) {
		return  $res->content;
	}
	else {
        print STDERR "\nerror request: $url";
        print STDERR "\nresponse:".$res->status_line( );
        print STDERR "\ncontent:".$res->content( );
		return;
	} 


}

sub deleteResource{
    my $self = shift;
	my $ua = LWP::UserAgent->new;
	#get resource list
    #/misc/category/name/{cname}/resource/name/{rname}/data GET
    
	my $url =$self->{_url}.'/rest/resources/resource/'.$_[0];
	my $req = HTTP::Request->new('DELETE',$url);
	$req->header($self->getHeaders()); #doesn't seems to work
    $req->authorization_basic($self->{_user}, $self->{_password});   
    #print $req->headers_as_string; #DEBUG
	my $res = $ua->request($req);
	# check the outcome
	if ($res->is_success) {
		return $res->is_success;
	}
	else {
        print STDERR "error request: $url";
        print STDERR "\nresponse:".$res->status_line( );
        print STDERR "\ncontent:".$res->content( );
		return;
	} #TODO ERROR management

}
sub getHeaders{
    my( $self ) = @_;
    
    return (
        'Accept' => 'application/json',
        'Content-Type' => 'application/json')
}

sub getBasicAuth{
    my( $self ) = @_;
    return $self->{_authentication};
}

sub setBasicAuth{
    my ( $self, $user, $pw ) = @_;
    $self->{_user} = $user if defined($user);
    $self->{_password} = $pw if defined($pw);
    return $self->{_authentication};
}

sub downloadResourceData{
    my $url =$self->{_url}.'/rest/misc/category/name/'. $_[0] . '/resource/name/'. $_[1] .'/data';
    my $ua = LWP::UserAgent->new(); 
    my $status = getstore($url, $_[2]);
    die "Error $status on $url" unless is_success($status); ## TODO use close_error
}
1;