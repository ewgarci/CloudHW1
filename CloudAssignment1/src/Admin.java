
import java.util.ArrayList;
import java.util.Arrays;
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
import com.amazonaws.services.ec2.model.DescribeImageAttributeRequest;
import com.amazonaws.services.ec2.model.DescribeImagesRequest;
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
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;



public class Admin {
	
		
	public static void main(String[] args) throws Exception {

		AWSCredentials credentials = new PropertiesCredentials(
				Admin.class.getResourceAsStream("AwsCredentials.properties"));
		AmazonEC2 ec2 = new AmazonEC2Client(credentials);
		AmazonS3Client s3  = new AmazonS3Client(credentials);

		DateFormat dateFormat = new SimpleDateFormat("_MMddyy_HHmmss");
		//get current date time with Date()
		Date date = new Date();


		String securityGroup = "WorkSecurity" + dateFormat.format(date);
		String keyName = "my_key" + dateFormat.format(date);
		//String keyPath = "c://";
		String zone = "us-east-1a";
		String imageId = "ami-76f0061f";
		String bucketName = "workcluster789";

		createSecurityGroup(ec2, securityGroup);
		createKey(keyName, ec2);
		//createBucket(s3, bucketName, zone);
		

		OnDemandAWS bob = new OnDemandAWS(keyName, securityGroup, zone, imageId, "bob-PC");
		//OnDemandAWS alice = new OnDemandAWS(keyName, securityGroup, zone, imageId, "alice-PC");

		//alice.createInstance();
			

		
		
		int days = 1;
		int numberOfChecks = 0;
		long pingCW = 30*1000;
		while (true) {
			
			if (isStartOfDay(numberOfChecks)) {
				numberOfChecks++;
				System.out.println("DAY: " + days);				
				
				//Recreate all instances and start them up;
				bob.createInstance();			
				
				//Sleep before starting up
				Thread.sleep(10*1000);
				
				System.out.println("Start up");
				bob.startUpOnDemandAWS();
				System.out.println("Attach EBS");
				bob.attachEBS();
				//bob.attachS3(bucketName);
				
				//Sleep for 30sec before pinging CloudWatch for the first time
				Thread.sleep(pingCW);
				continue;
			} else if (isEndOfDay(numberOfChecks)) {
				numberOfChecks = 0;
				days++;
				
				/*Terminate all instances*/

				//Try to shut down the machine		
				System.out.println("Detach EBS");
				bob.detachEBS();
				System.out.println("Snapshot");
				
				bob.saveSnapShot();		
				//wait for the snapshot to be created and then shutdown the machine
				while(!bob.getSnapShotState().equalsIgnoreCase("available")){
					Thread.sleep(15*1000);
				}
				System.out.println("Snapshot created");
				
				System.out.println("Terminate");		
				bob.shutDownOnDemandAWS();
				
				/*Terminate all instances*/

				if (days > 2)
					break;
				
				System.out.println("SLEEP");				
				//Sleep for the night - 2min
				Thread.sleep(30 * 1000);				
				continue;
			}

			//See which machine is idle and try to shut it down.
			numberOfChecks++;
			
			System.out.println("Check: " + numberOfChecks);
			
			//Sleep for 30sec before pinging CloudWatch again
			Thread.sleep(pingCW);
		}
	}
	
	private static Boolean isStartOfDay(int numberOfChecks){
		return numberOfChecks == 0;
	}
	
	private static Boolean isEndOfDay(int numberOfChecks){
		return numberOfChecks > 3;
	}
	
	
	
	private static void createKey(String keyName, AmazonEC2 ec2){
		try {
		    CreateKeyPairRequest newKeyRequest = new CreateKeyPairRequest();
		    newKeyRequest.setKeyName(keyName);
		    CreateKeyPairResult keyresult = ec2.createKeyPair(newKeyRequest);
		    //KeyPair keyPair = new KeyPair();
		    //keyPair = keyresult.getKeyPair();
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
	
	public static void createBucket(AmazonS3Client s3, String bucketName, String zone){
		try{
		
		List <Bucket> bucketList = s3.listBuckets();

        for (Bucket bucket : bucketList){
        	System.out.println(bucket.getName());
        	if ( bucketName == bucket.getName() ){
        		return;
        	}
        }
        
        s3.createBucket(bucketName);
        		
        return;	
        	
		}catch (AmazonServiceException ase) {
            System.out.println("Caught Exception: " + ase.getMessage());
            System.out.println("Reponse Status Code: " + ase.getStatusCode());
            System.out.println("Error Code: " + ase.getErrorCode());
            System.out.println("Request ID: " + ase.getRequestId());
		}
	}
	
	public static void createSecurityGroup(AmazonEC2 ec2, String securityGroup){
		
		CreateSecurityGroupRequest createSecurityGroupRequest = 
				new CreateSecurityGroupRequest();
		
		
		
		createSecurityGroupRequest.withGroupName(securityGroup)
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
			    	
		authorizeSecurityGroupIngressRequest.withGroupName(securityGroup)
		                                    .withIpPermissions(permissions);
			        
		ec2.authorizeSecurityGroupIngress(authorizeSecurityGroupIngressRequest);
	}
}
