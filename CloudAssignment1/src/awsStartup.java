
/*
 * Copyright 2010 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 * 
 * 
 * Modified by Kyung-Hwa Kim (kk2515@columbia.edu)
 * Modified by Sambit Sahu
 * 
 */
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
//import com.amazonaws.services.ec2.model.DisassociateAddressRequest;


import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
//import com.amazonaws.services.ec2.model.AuthorizeSecurityGroupIngressRequest;
//import com.amazonaws.services.ec2.model.CreateKeyPairRequest;
//import com.amazonaws.services.ec2.model.CreateKeyPairResult;
//import com.amazonaws.services.ec2.model.CreateSecurityGroupRequest;
//import com.amazonaws.services.ec2.model.CreateSecurityGroupResult;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.DescribeAvailabilityZonesResult;
//import com.amazonaws.services.ec2.model.DescribeImagesResult;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.DescribeKeyPairsResult;
//import com.amazonaws.services.ec2.model.Image;
import com.amazonaws.services.ec2.model.Placement;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceState;
//import com.amazonaws.services.ec2.model.IpPermission;
//import com.amazonaws.services.ec2.model.KeyPair;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
//import com.amazonaws.services.ec2.model.StartInstancesRequest;
//import com.amazonaws.services.ec2.model.StopInstancesRequest;
import com.amazonaws.services.ec2.model.Tag;
//import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.services.ec2.model.AttachVolumeRequest;
import com.amazonaws.services.ec2.model.CreateVolumeRequest;
import com.amazonaws.services.ec2.model.CreateVolumeResult;
//import com.amazonaws.services.ec2.model.DetachVolumeRequest;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;

public class awsStartup {

    /*
     * Important: Be sure to fill in your AWS access credentials in the
     *            AwsCredentials.properties file before you try to run this
     *            sample.
     * http://aws.amazon.com/security-credentials
     */

    static AmazonEC2      ec2;
    static AmazonS3Client s3;
    
    //Test commit comment
    public static void main(String[] args) throws Exception {


    	 AWSCredentials credentials = new PropertiesCredentials(
    			 	awsStartup.class.getResourceAsStream("AwsCredentials.properties"));

         /*********************************************
          * 
          *  #1 Create Amazon Client object
          *  
          *********************************************/
    	 System.out.println("#1 Create Amazon Client object");
         ec2 = new AmazonEC2Client(credentials);
         String createdInstanceId = null;
         DateFormat dateFormat = new SimpleDateFormat("_MMddyy_HHmmss");
		   //get current date time with Date()
		   Date date = new Date();
		   
		  //String keyName = "myKey" + dateFormat.format(date);
		  //String securityGroup = "JavaSecurityGroup" + dateFormat.format(date);
		  //String securityGroup = "JavaSecurityGroup";
		  String machineName = "Instance" + dateFormat.format(date);
         
       
        try {
        	
        	/*********************************************
        	 * 
             *  #2 Describe Availability Zones.
             *  
             *********************************************/
        	System.out.println("#2 Describe Availability Zones.");
            DescribeAvailabilityZonesResult availabilityZonesResult = ec2.describeAvailabilityZones();
            System.out.println("You have access to " + availabilityZonesResult.getAvailabilityZones().size() +
                    " Availability Zones.");
            Placement placement = new Placement();
            placement.setAvailabilityZone("us-east-1a");
             
            
            
            /*********************************************
             * 
             *  #3 Describe Available Images
             *  
             *********************************************/
            //System.out.println("#3 Describe Available Images");
            //DescribeImagesResult dir = ec2.describeImages();
            //List<Image> images = dir.getImages();
            //System.out.println("You have " + images.size() + " Amazon images");
            
            
            /*********************************************
             *                 
             *  #4 Describe Key Pair
             *                 
             *********************************************/
            System.out.println("#4 Describe Key Pair");
            DescribeKeyPairsResult dkr = ec2.describeKeyPairs();
            System.out.println(dkr.toString());
            
            /*********************************************
             * 
             *  #5 Describe Current Instances
             *  
             *********************************************/
            System.out.println("#5 Describe Current Instances");
            DescribeInstancesResult describeInstancesRequest = ec2.describeInstances();
            List<Reservation> reservations = describeInstancesRequest.getReservations();
            Set<Instance> instances = new HashSet<Instance>();
            // add all instances to a Set.
            for (Reservation reservation : reservations) {
            	instances.addAll(reservation.getInstances());
            }
            
            System.out.println("You have " + instances.size() + " Amazon EC2 instance(s).");
            for (Instance ins : instances){
            	
            	// instance id
            	String instanceId = ins.getInstanceId();
            	
            	
            	// instance state
            	InstanceState is = ins.getState();
            	System.out.println(instanceId+" "+is.getName());
            }
     
            /*********************************************
             * 
             *  #6 Create a Key Pair
             *  
             *********************************************/
            //System.out.println("#5 Create a Key Pairs");
            //CreateKeyPairRequest newKeyRequest = new CreateKeyPairRequest();
            //newKeyRequest.setKeyName(keyName);
            //CreateKeyPairResult keyresult = ec2.createKeyPair(newKeyRequest);
            
            //KeyPair keyPair = new KeyPair();
            //keyPair = keyresult.getKeyPair();
            //String privateKey = keyPair.getKeyMaterial();
            
            //System.out.println("The key is = " + keyPair.toString());
            
                        
            /*********************************************
             * 
             *  #6 Create a Security Group
             *  
             *********************************************/
            /*
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
        	
        	List<String> securityGroups = new ArrayList<String>();
        	securityGroups.add(securityGroup);
        	*/
        	List<String> securityGroups = new ArrayList<String>();
        	securityGroups.add("JavaSecurityGroup");
            
            /*********************************************
             * 
             *  #6 Create an Instance
             *  
             *********************************************/
        	
            System.out.println("#6 Create an Instance");
            String imageId = "ami-76f0061f"; //Basic 32-bit Amazon Linux AMI
            int minInstanceCount = 1; // create 1 instance
            int maxInstanceCount = 1;
            RunInstancesRequest rir = new RunInstancesRequest(imageId, minInstanceCount, maxInstanceCount);
            rir.setKeyName("my_key");
            rir.setSecurityGroups(securityGroups);
            rir.setPlacement(placement);
            RunInstancesResult result = ec2.runInstances(rir);
            
            //get instanceId from the result
            List<Instance> resultInstance = result.getReservation().getInstances();
            
            for (Instance ins : resultInstance){
            	createdInstanceId = ins.getInstanceId();
            	System.out.println("New instance has been created: "+ins.getInstanceId());
      //        System.out.println("getKeyName() is: "+ ins.setKeyName(myEC2key));
            }
            
            
            /*********************************************
             * 
             *  #7  Show the Public-DNS
             *  
             *********************************************/
 
            
            describeInstancesRequest = ec2.describeInstances();
            reservations = describeInstancesRequest.getReservations();
            instances = new HashSet<Instance>();
            // add all instances to a Set.
            for (Reservation reservation : reservations) {
            	instances.addAll(reservation.getInstances());
            }
 
                      
            System.out.println("You have " + instances.size() + " Amazon EC2 instance(s).");
            for (Instance ins : instances){
            	
            	// instance id
            	String instanceId = ins.getInstanceId();
            	
            	// instance state
            	InstanceState is = ins.getState();
            	
            	while (is.getName().equals("pending")){
            		Thread.sleep(30000);
            		ins = getInstance(createdInstanceId);
            		is = ins.getState();
            	}
            	
            	System.out.println(instanceId+" "+is.getName());
            	
            	System.out.println("Public DNS " + ins.getPublicDnsName());
                System.out.println("Private IP " + ins.getPrivateIpAddress());
                System.out.println("Public IP " + ins.getPublicIpAddress());
            }
            
            
            
            
            /*********************************************
             * 
             *  #7 Create a 'tag' for the new instance.
             *  
             *********************************************/
            System.out.println("#7 Create a 'tag' for the new instance.");
            List<String> resources = new LinkedList<String>();
            List<Tag> tags = new LinkedList<Tag>();
            Tag nameTag = new Tag("Name", machineName);
            
            resources.add(createdInstanceId);
            tags.add(nameTag);
            
            CreateTagsRequest ctr = new CreateTagsRequest(resources, tags);
            ec2.createTags(ctr);
            
            
            // we assume that we've already created an instance. Use the id of the instance.
         	String instanceId = createdInstanceId; //put your own instance id to test this code.
            
        	/*********************************************
             *  #2.1 Create a volume
             *********************************************/
          	//create a volume
         	CreateVolumeRequest cvr = new CreateVolumeRequest();
 	        cvr.setAvailabilityZone("us-east-1a");
 	        cvr.setSize(10); //size = 10 gigabytes
         	CreateVolumeResult volumeResult = ec2.createVolume(cvr);
         	String createdVolumeId = volumeResult.getVolume().getVolumeId();
          	
         	
         	/*********************************************
             *  #2.2 Attach the volume to the instance
             *********************************************/
         	AttachVolumeRequest avr = new AttachVolumeRequest();
         	avr.setVolumeId(createdVolumeId);
         	avr.setInstanceId(instanceId);
         	avr.setDevice("/dev/sdf");
         	ec2.attachVolume(avr);
         	
         	/*********************************************
             *  #2.3 Detach the volume from the instance
             *********************************************/
         	//DetachVolumeRequest dvr = new DetachVolumeRequest();
         	//dvr.setVolumeId(createdVolumeId);
         	//dvr.setInstanceId(instanceId);
         	//ec2.detachVolume(dvr);
         	
         	
             /************************************************
             *    #3 S3 bucket and object
             ***************************************************/
             s3  = new AmazonS3Client(credentials);
             
             //create bucket
             String bucketName = "cloudsampleewg09";
             s3.createBucket(bucketName);
             
             //set key
             String key = "object-name.txt";
             
             //set value
             File file = File.createTempFile("temp", ".txt");
             //file.deleteOnExit();
             Writer writer = new OutputStreamWriter(new FileOutputStream(file));
             writer.write("This is a sample sentence.\r\nYes!");
             writer.close();
             
             //put object - bucket, key, value(file)
             s3.putObject(new PutObjectRequest(bucketName, key, file));
             
             //get object
             S3Object object = s3.getObject(new GetObjectRequest(bucketName, key));
             BufferedReader reader = new BufferedReader(
             	    new InputStreamReader(object.getObjectContent()));
             String dataS3 = null;
             while ((dataS3 = reader.readLine()) != null) {
                 System.out.println(dataS3);
             }
            
                       
        	/*********************************************
 			*  	#8 Allocate elastic IP addresses.
 			*********************************************/
            
          
 			
 			//allocate
 			AllocateAddressResult elasticResult = ec2.allocateAddress();
 			String elasticIp = elasticResult.getPublicIp();
 			System.out.println("New elastic IP: "+elasticIp);
 				
 			//associate
 			AssociateAddressRequest aar = new AssociateAddressRequest();
 			aar.setInstanceId(instanceId);
 			aar.setPublicIp(elasticIp);
 			ec2.associateAddress(aar);
 			
 			//disassociate
 			//DisassociateAddressRequest dar = new DisassociateAddressRequest();
 			//dar.setPublicIp(elasticIp);
 			//ec2.disassociateAddress(dar);
             
         	
 			/***********************************
 			 *   #9 Monitoring (CloudWatch)
 			 *********************************/
 			
 			//create CloudWatch client
 			AmazonCloudWatchClient cloudWatch = new AmazonCloudWatchClient(credentials) ;
 			
 			//create request message
 			GetMetricStatisticsRequest statRequest = new GetMetricStatisticsRequest();
 			
 			//set up request message
 			statRequest.setNamespace("AWS/EC2"); //namespace
 			statRequest.setPeriod(60); //period of data
 			ArrayList<String> stats = new ArrayList<String>();
 			
 			//Use one of these strings: Average, Maximum, Minimum, SampleCount, Sum 
 			stats.add("Average"); 
 			stats.add("Sum");
 			statRequest.setStatistics(stats);
 			
 			//Use one of these strings: CPUUtilization, NetworkIn, NetworkOut, DiskReadBytes, DiskWriteBytes, DiskReadOperations  
 			statRequest.setMetricName("CPUUtilization"); 
 			
 			// set time
 			GregorianCalendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
 			calendar.add(GregorianCalendar.SECOND, -1 * calendar.get(GregorianCalendar.SECOND)); // 1 second ago
 			Date endTime = calendar.getTime();
 			calendar.add(GregorianCalendar.MINUTE, -10); // 10 minutes ago
 			Date startTime = calendar.getTime();
 			statRequest.setStartTime(startTime);
 			statRequest.setEndTime(endTime);
 			
 			//specify an instance
 			ArrayList<Dimension> dimensions = new ArrayList<Dimension>();
 			dimensions.add(new Dimension().withName("InstanceId").withValue(instanceId));
 			statRequest.setDimensions(dimensions);
 			
 			//get statistics
 			GetMetricStatisticsResult statResult = cloudWatch.getMetricStatistics(statRequest);
 			
 			//display
 			System.out.println(statResult.toString());
 			List<Datapoint> dataList = statResult.getDatapoints();
 			Double averageCPU = null;
 			Date timeStamp = null;
 			for (Datapoint data : dataList){
 				averageCPU = data.getAverage();
 				timeStamp = data.getTimestamp();
 				System.out.println("Average CPU utlilization for last 10 minutes since " +timeStamp.toString() + ": " +averageCPU);
 				System.out.println("Total CPU utlilization for last 10 minutes since "+timeStamp.toString() + ": " + data.getSum());
 			}
                 
            
                        
            /*********************************************
             * 
             *  #8 Stop/Start an Instance
             *  
             *********************************************/
            //System.out.println("#8 Stop the Instance");
            //List<String> instanceIds = new LinkedList<String>();
            //instanceIds.add(createdInstanceId);
            
            //stop
            //StopInstancesRequest stopIR = new StopInstancesRequest(instanceIds);
            //ec2.stopInstances(stopIR);
            
            //start
            //StartInstancesRequest startIR = new StartInstancesRequest(instanceIds);
            //ec2.startInstances(startIR);
            
            
            /*********************************************
             * 
             *  #9 Terminate an Instance
             *  
             *********************************************/
            //System.out.println("#9 Terminate the Instance");
            //TerminateInstancesRequest tir = new TerminateInstancesRequest(instanceIds);
            //ec2.terminateInstances(tir);
            
            		    		
            /*********************************************
             *  
             *  #10 shutdown client object
             *  
             *********************************************/
           // System.out.println("#10 Shutdown");
           // ec2.shutdown();
            
            
            
        } catch (AmazonServiceException ase) {
                System.out.println("Caught Exception: " + ase.getMessage());
                System.out.println("Reponse Status Code: " + ase.getStatusCode());
                System.out.println("Error Code: " + ase.getErrorCode());
                System.out.println("Request ID: " + ase.getRequestId());
        }
        
        
    }

    
    public static Instance getInstance(String id){
    	
        DescribeInstancesResult describeInstancesRequest = ec2.describeInstances();
        List<Reservation> reservations = describeInstancesRequest.getReservations();
        Set<Instance> instances = new HashSet<Instance>();
        // add all instances to a Set.
        for (Reservation reservation : reservations) {
        	instances.addAll(reservation.getInstances());
        }
        
                
        for ( Instance ins : instances){
           	if ( id.equalsIgnoreCase(ins.getInstanceId()) == true ){
        		return ins;
        	}
        }
        
        return null;
        
        
            	
    }
        
    
}

