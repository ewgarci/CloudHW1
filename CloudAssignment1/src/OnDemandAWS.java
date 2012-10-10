import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.Datapoint;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsRequest;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsResult;
import com.amazonaws.services.ec2.model.AllocateAddressResult;
import com.amazonaws.services.ec2.model.AssociateAddressRequest;
import com.amazonaws.services.ec2.model.DisassociateAddressRequest;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.AuthorizeSecurityGroupIngressRequest;
import com.amazonaws.services.ec2.model.CreateKeyPairRequest;
import com.amazonaws.services.ec2.model.CreateKeyPairResult;
import com.amazonaws.services.ec2.model.CreateSecurityGroupRequest;
import com.amazonaws.services.ec2.model.CreateSecurityGroupResult;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.DescribeAvailabilityZonesResult;
import com.amazonaws.services.ec2.model.DescribeImagesResult;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.DescribeKeyPairsResult;
import com.amazonaws.services.ec2.model.Image;
import com.amazonaws.services.ec2.model.Placement;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceState;
import com.amazonaws.services.ec2.model.IpPermission;
import com.amazonaws.services.ec2.model.KeyPair;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.StartInstancesRequest;
import com.amazonaws.services.ec2.model.StopInstancesRequest;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.services.ec2.model.AttachVolumeRequest;
import com.amazonaws.services.ec2.model.CreateVolumeRequest;
import com.amazonaws.services.ec2.model.CreateVolumeResult;
import com.amazonaws.services.ec2.model.DetachVolumeRequest;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
	
public class OnDemandAWS {
	
	private AmazonEC2 ec2;
    private AmazonS3Client s3;
    private AWSCredentials credentials;
    private String instanceId;
    private String keyName;
	private String securityGroup;
	private String machineName;
	private static String keyPath;
	private String zone;
	private String imageId ;	
	
	public OnDemandAWS(String keyName, String securityGroup, String zone, String imageId, String machineName) throws IOException{
		
		credentials = new PropertiesCredentials(
			 	awsStartup.class.getResourceAsStream("AwsCredentials.properties"));
		
		try {
			
			//ec2 = new AmazonEC2Client(credentials);
			//s3  = new AmazonS3Client(credentials);
			
			
			keyName = this.keyName;
			securityGroup = this.securityGroup;
			zone = this.zone;
			imageId= this.imageId;
			
						
			Placement placement = new Placement();
            placement.setAvailabilityZone(zone);
			
            List<String> securityGroups = new ArrayList<String>();
        	securityGroups.add(securityGroup);
            
            int minInstanceCount = 1; // create 1 instance
            int maxInstanceCount = 1;
            RunInstancesRequest rir = new RunInstancesRequest(imageId, minInstanceCount, maxInstanceCount);
            rir.setKeyName(keyName);
            rir.setSecurityGroups(securityGroups);
            rir.setPlacement(placement);
            RunInstancesResult result = ec2.runInstances(rir);
            
            //get instanceId from the result
            List<Instance> resultInstance = result.getReservation().getInstances();
            
            for (Instance ins : resultInstance){
            	instanceId = ins.getInstanceId();
            	System.out.println("New instance has been created: "+ins.getInstanceId());
            }
            
            
           
			
		} catch (AmazonServiceException ase) {
            System.out.println("Caught Exception: " + ase.getMessage());
            System.out.println("Reponse Status Code: " + ase.getStatusCode());
            System.out.println("Error Code: " + ase.getErrorCode());
            System.out.println("Request ID: " + ase.getRequestId());
		}
		
		
		
	}
	
	public Instance getInstance(){
    	
        DescribeInstancesResult describeInstancesRequest = ec2.describeInstances();
        List<Reservation> reservations = describeInstancesRequest.getReservations();
        Set<Instance> instances = new HashSet<Instance>();
        // add all instances to a Set.
        for (Reservation reservation : reservations) {
        	instances.addAll(reservation.getInstances());
        }
        
                
        for ( Instance ins : instances){
           	if ( imageId.equalsIgnoreCase(ins.getInstanceId()) == true ){
        		return ins;
        	}
        }
        
        return null;
        
        
            	
    }
	
	
	//public String getPublicDNS(){
    	                	
    //}
    
	
	public void assignElasticIp(String ipAddress){
			AssociateAddressRequest aar = new AssociateAddressRequest();
			aar.setInstanceId(instanceId);
			aar.setPublicIp(ipAddress);
			ec2.associateAddress(aar);
	}
	
	public void disassociateElasticIp(String ipAddress){	
		DisassociateAddressRequest dar = new DisassociateAddressRequest();
		dar.setPublicIp(ipAddress);
		ec2.disassociateAddress(dar);
	}
	
	public void shutDownOnDemandAWS(){
		
	}
	
	public void startUpOnDemandAWS(){
		
	}
	
	public void createEBS(){
		
	}
	
	public void attachEBS(){
		
	}
	
	public void removeEBS(){
		
	}
	
	public void attachS3(){
		
	}
	
	public void saveSnapShot(){
		
	}
	
	
	/*
	private static void createKey(String keyName){
		try {
		    CreateKeyPairRequest newKeyRequest = new CreateKeyPairRequest();
		    newKeyRequest.setKeyName(keyName);
		    CreateKeyPairResult keyresult = ec2.createKeyPair(newKeyRequest);
		    KeyPair keyPair = new KeyPair();
		    keyPair = keyresult.getKeyPair();
		    String privateKey = keyPair.getKeyMaterial();
		    writeKeytoFile(keyPath, privateKey);
		    
		} catch (AmazonServiceException ase) {
            System.out.println("Caught Exception: " + ase.getMessage());
            System.out.println("Reponse Status Code: " + ase.getStatusCode());
            System.out.println("Error Code: " + ase.getErrorCode());
            System.out.println("Request ID: " + ase.getRequestId());
		}
	}
	
	private static void writeKeytoFile(String path, String privateKey){
		
		
		
	}
	*/
	
	
}
