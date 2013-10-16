use GeoStoreClient;
use LWP::UserAgent; 
use LWP::Simple;
use lib 'lib/';
use JSON;
use LWP::UserAgent;
use HTTP::Request::Common qw(POST); 
use HTTP::Request::Common qw(GET); 
use utility;

my $gsUser="admin";
my $gsPassword="admin";
my $geostoreUrl="http://localhost/geostore";
#setup geostore client
my $gsClient = GeoStoreClient->new($geostoreUrl);
$gsClient->setBasicAuth($gsUser,$gsPassword);
my $resource =$gsClient->getResource(41);
print ( $resource );
print ( $gsClient->updateAttribute($resource,"statusMessage","success")); 