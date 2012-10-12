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
import com.amazonaws.services.ec2.model.CreateImageRequest;
import com.amazonaws.services.ec2.model.CreateImageResult;
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
import com.amazonaws.services.ec2.model.InstanceType;
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
	
	AmazonEC2 ec2;
	AmazonS3Client s3;
	AWSCredentials credentials;
	String instanceId;
	String keyName;
	String securityGroup;
	String machineName;
	String zone;
	String imageId;	
	String volumeId;
	String ipAddress;
	
	
	public OnDemandAWS(String keyName, String securityGroup, String zone, String imageId, String machineName) throws IOException{
		
		credentials = new PropertiesCredentials(
			 	awsStartup.class.getResourceAsStream("AwsCredentials.properties"));
		
		ec2 = new AmazonEC2Client(credentials);
		s3  = new AmazonS3Client(credentials);		
		
		this.keyName = keyName;
		this.securityGroup = securityGroup;
		this.zone = zone;
		this.imageId= imageId;
		this.machineName= machineName;
		this.createEBS(10);		
	}
	
	public void createInstance() {
		try {			
						
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
            rir.setMonitoring(true);
            rir.setInstanceType(InstanceType.T1Micro);
            
            RunInstancesResult result = ec2.runInstances(rir);
            
            //get instanceId from the result
            List<Instance> resultInstance = result.getReservation().getInstances();
            
            InstanceState is = null;
            
            for (Instance ins : resultInstance){
            	instanceId = ins.getInstanceId();
            	is = ins.getState();
            }        
            
            
            
            List<String> resources = new LinkedList<String>();
            List<Tag> tags = new LinkedList<Tag>();
            Tag nameTag = new Tag("Name", machineName);
            
            resources.add(instanceId);
            tags.add(nameTag);
            
            CreateTagsRequest ctr = new CreateTagsRequest(resources, tags);
            ec2.createTags(ctr);
            
            while (is.getName().equals("pending")){
        		try {
					Thread.sleep(30000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
        		is = this.getInstance().getState();
        	}
            
            if (this.ipAddress == null){
            	this.createElasticIp();
            }
            
            
            this.assignElasticIp();
            
            
            System.out.println(machineName + " id = " + this.instanceId + " ip = " + this.ipAddress);
           
			
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
           	if ( instanceId.equalsIgnoreCase(ins.getInstanceId()) == true ){
        		return ins;
        	}
        }
        
        return null;
        
    }
	    
	
	public void createElasticIp(){
		AllocateAddressResult elasticResult = ec2.allocateAddress();
		String elasticIp = elasticResult.getPublicIp();
		this.ipAddress = elasticIp;
	}
	
	public void assignElasticIp(){
		AssociateAddressRequest aar = new AssociateAddressRequest();
		aar.setInstanceId(this.instanceId);
		aar.setPublicIp(this.ipAddress);
		ec2.associateAddress(aar);
	}
	
	public void disassociateElasticIp(){	
		DisassociateAddressRequest dar = new DisassociateAddressRequest();
		dar.setPublicIp(this.ipAddress);
		ec2.disassociateAddress(dar);
	}
	
	//Save a snapshot of the machine and terminate it
    public void shutDownOnDemandAWS(){
//        this.saveSnapShot();
//        
//        StopInstancesRequest sir = new StopInstancesRequest();
//        sir.setInstanceIds(Arrays.asList(this.instanceId));
        
        TerminateInstancesRequest tir = new TerminateInstancesRequest(Arrays.asList(this.instanceId));
        ec2.terminateInstances(tir);
    }
	
	public void startUpOnDemandAWS(){
        //start
        StartInstancesRequest startIR = new StartInstancesRequest(Arrays.asList(this.instanceId));
        ec2.startInstances(startIR);
	}
	
	 public String createEBS(Integer size){
        /*********************************************
         * Create a volume
         *********************************************/
         CreateVolumeRequest cvr = new CreateVolumeRequest();
         cvr.setAvailabilityZone(zone);// "us-east-1a"
         cvr.setSize(size); //size = 10 gigabytes
         CreateVolumeResult volumeResult = ec2.createVolume(cvr);
         String createdVolumeId = volumeResult.getVolume().getVolumeId();//"vol-b51510cf"
         
         this.volumeId = createdVolumeId;
      
         List<String> resources = new LinkedList<String>();
         List<Tag> tags = new LinkedList<Tag>();
         Tag nameTag = new Tag("Name", machineName);
         
         resources.add(this.volumeId);
         tags.add(nameTag);
         
         CreateTagsRequest ctr = new CreateTagsRequest(resources, tags);
         ec2.createTags(ctr);
         
         return createdVolumeId;
	}
	
	public void attachEBS(){
        /*********************************************
         * Attach the volume to the instance
         *********************************************/
         AttachVolumeRequest avr = new AttachVolumeRequest();
         avr.setVolumeId(this.volumeId);
         avr.setInstanceId(this.instanceId);
         avr.setDevice("/dev/sdf");
         ec2.attachVolume(avr);        
	}
	    
    public void detachEBS() {
        /*********************************************
         * Detach the volume from the instance
         *********************************************/
         DetachVolumeRequest dvr = new DetachVolumeRequest();
         dvr.setVolumeId(this.volumeId);
         dvr.setInstanceId(instanceId);
         ec2.detachVolume(dvr);
    }
	
	public void attachS3(String bucketName) throws IOException {
        
        
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
	}

	 //Creates a snapshot of the machine and returns the AMI
    public String saveSnapShot() {
            /***********************************
         * Create an AMI from an instance
         *********************************/
    	
        CreateImageRequest cir = new CreateImageRequest();
        cir.setInstanceId(instanceId);
        cir.setName(instanceId + "__AMI"); //we can choose the name
        
        CreateImageResult createImageResult = ec2.createImage(cir);
        String createdImageId = createImageResult.getImageId();
        
        System.out.println("Sent creating AMI request. AMI id=" + createdImageId);
        
        //Save the AMI
        this.imageId = createdImageId;
        
        return createdImageId;
    }
    
    //Should be invoked after the saveSnapShot has been called
    public String getSnapShotState() {
		DescribeImagesRequest dir = new DescribeImagesRequest();
		dir.setImageIds(Arrays.asList(this.imageId));
		String state = ec2.describeImages(dir).getImages().get(0).getState();
		
		return state;
    }
    
    
}
