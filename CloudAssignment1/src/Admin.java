
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
//import com.amazonaws.services.dynamodb.datamodeling.KeyPair;
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



public class Admin {
	
		
	public static void main(String[] args) throws IOException {
		
	    AWSCredentials credentials = new PropertiesCredentials(
		awsStartup.class.getResourceAsStream("AwsCredentials.properties"));
	    AmazonEC2 ec2 = new AmazonEC2Client(credentials);
	    AmazonS3Client s3  = new AmazonS3Client(credentials);
	    
	     String securityGroup = "WorkSecurity";
		 String keyName = "my_key";
		 //String keyPath = "c://";
		 String zone = "us-east-1a";
		 String imageId = "ami-76f0061f";
	    
	    createSecurityGroup(ec2);
	    createKey(keyName, ec2);
	    
	    
	    
	    OnDemandAWS bob = new OnDemandAWS(keyName, securityGroup, zone, imageId, "bob-PC");
	    OnDemandAWS alice = new OnDemandAWS(keyName, securityGroup, zone, imageId, "alice-PC");
		
	}
	
	private static void createKey(String keyName, AmazonEC2 ec2){
		try {
		    CreateKeyPairRequest newKeyRequest = new CreateKeyPairRequest();
		    newKeyRequest.setKeyName(keyName);
		    CreateKeyPairResult keyresult = ec2.createKeyPair(newKeyRequest);
		    KeyPair keyPair = new KeyPair();
		    keyPair = keyresult.getKeyPair();
		    //String privateKey = keyPair.getKeyMaterial();
		    //writeKeytoFile(keyPath, privateKey);
		    
		} catch (AmazonServiceException ase) {
            System.out.println("Caught Exception: " + ase.getMessage());
            System.out.println("Reponse Status Code: " + ase.getStatusCode());
            System.out.println("Error Code: " + ase.getErrorCode());
            System.out.println("Request ID: " + ase.getRequestId());
		}
	}
	
	private void writeKeytoFile(String path, String privateKey) {
	
	}
	
	// Setup the CloudWatch client and setup the metrics that we want to get
	public void setupCloudWatch() {
	}
	
	public void getCPUUsage(String instanceId){	
	}
	
	
	
	public static void createSecurityGroup(AmazonEC2 ec2){
		
		CreateSecurityGroupRequest createSecurityGroupRequest = 
				new CreateSecurityGroupRequest();
		
		
		
		createSecurityGroupRequest.withGroupName("My Java Security Group")
				.withDescription("My Java Security Group");
		    	
		    CreateSecurityGroupResult createSecurityGroupResult = 
		    		ec2.createSecurityGroup(createSecurityGroupRequest);
		  
		    
		    //SSH
		IpPermission ipPermission1 = new IpPermission();
		ipPermission1.withIpRanges("0.0.0.0/0")
		            .withIpProtocol("tcp")
		            .withFromPort(22)
		            .withToPort(22);
		//http
		IpPermission ipPermission2 = new IpPermission();
		ipPermission2.withIpRanges("0.0.0.0/0")
		            .withIpProtocol("tcp")
		            .withFromPort(80)
		            .withToPort(80);
		//https
		IpPermission ipPermission3 = new IpPermission();
		ipPermission3.withIpRanges("0.0.0.0/0")
		            .withIpProtocol("tcp")
		            .withFromPort(443)
		            .withToPort(443);
		//tcp
		IpPermission ipPermission4 = new IpPermission();
		ipPermission4.withIpRanges("0.0.0.0/0")
		            .withIpProtocol("tcp")
		            .withFromPort(65535)
		            .withToPort(65535);
		
		List<IpPermission> permissions = new ArrayList<IpPermission>();
		permissions.add(ipPermission1);
		permissions.add(ipPermission2);
		permissions.add(ipPermission3);
		permissions.add(ipPermission4);
		
		AuthorizeSecurityGroupIngressRequest authorizeSecurityGroupIngressRequest =
				new AuthorizeSecurityGroupIngressRequest();
			    	
		authorizeSecurityGroupIngressRequest.withGroupName("My Java Security Group")
		                                    .withIpPermissions(permissions);
			        
		ec2.authorizeSecurityGroupIngress(authorizeSecurityGroupIngressRequest);
	}
}
