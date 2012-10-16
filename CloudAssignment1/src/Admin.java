
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
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.Writer;
import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;


import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.model.CreateAutoScalingGroupRequest;
import com.amazonaws.services.autoscaling.model.CreateLaunchConfigurationRequest;
import com.amazonaws.services.autoscaling.model.LaunchConfiguration;
import com.amazonaws.services.autoscaling.model.PutScalingPolicyRequest;
import com.amazonaws.services.autoscaling.model.PutScalingPolicyResult;
//import com.amazonaws.services.dynamodb.datamodeling.KeyPair;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.Datapoint;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsRequest;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsResult;
import com.amazonaws.services.cloudwatch.model.PutMetricAlarmRequest;
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
import com.amazonaws.services.ec2.model.InstanceType;
import com.amazonaws.services.ec2.model.KeyPairInfo;
import com.amazonaws.services.ec2.model.Placement;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceState;
import com.amazonaws.services.ec2.model.IpPermission;
import com.amazonaws.services.ec2.model.KeyPair;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.SecurityGroup;
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

	//Interval for pinging the CloudWatch for statistics - seconds
	public static int pingCW = 60;
	//Max number of days the VMs should be created and destroyed
	public static int maxDays = 3;
	//Duration of night seconds
	public static int nightDuration = 4*60;
	//Duration of day in seconds
	public static int dayDuration = 8*60;
	//The lower bound under which a VM is considered idle and therefore terminated
	public static double cpuIdle = 0.00;
	public static String keyPath = " ";
	
	//Upper threshold
	public static double upperT = 50.0;	
	//Lower threshold
	public static double lowerT = 20.0;
	
	public static void main(String[] args) throws Exception {

		AWSCredentials credentials = new PropertiesCredentials(
				Admin.class.getResourceAsStream("AwsCredentials.properties"));
		AmazonEC2 ec2 = new AmazonEC2Client(credentials);
		AmazonS3Client s3  = new AmazonS3Client(credentials);
		//AmazonAutoScalingClient autoScale  = new AmazonAutoScalingClient(credentials);

		AmazonCloudWatchClient cloudWatch = new AmazonCloudWatchClient(credentials);

		String securityGroup = "WorkSecurity";
		String keyName = "my_key2";
		String zone = "us-east-1a";
		String imageId = "ami-76f0061f";
		String bucketName = "pblcluster";//pblcluster workcluster789";

		createSecurityGroup(ec2, securityGroup);
		createKey(keyName, ec2);
		createBucket(s3, bucketName, zone);
		//setupAutoScale(autoScale, cloudWatch, keyName, zone, securityGroup, imageId);
		//setupPolicy(autoScale, cloudWatch);

		OnDemandAWS bob = new OnDemandAWS(keyName, securityGroup, zone, imageId, "bob-PC");
		bob.createEBS(10);
		OnDemandAWS alice = new OnDemandAWS(keyName, securityGroup, zone, imageId, "alice-PC");
		alice.createEBS(10);
		
		//For Auto Scaling
		OnDemandAWS bob2 = new OnDemandAWS(keyName, securityGroup, zone, imageId, "bob-PC-2");

	
		/*bob.createInstance();
		alice.createInstance();
		Thread.sleep(2*60*1000);
		//Before increasing CPU ssh needs to be initialized. This takes a around 2 minutes after the cpu starts
		increaseCPU(bob, keyName);
		increaseCPU(alice, keyName);

		int count = 0;
		while(true){
			System.out.println("bob cpu = " + getCPUUsage(cloudWatch, bob.instanceId));
			System.out.println("alice cpu = " + getCPUUsage(cloudWatch, alice.instanceId));
			Thread.sleep(60*1000);
			count++;
			if (count>3){
				stopCPU(bob, keyName);
				stopCPU(alice, keyName);
			}
		}*/
	 

		List<OnDemandAWS> machines = Arrays.asList(bob, alice);

		int days = 1;
		int numberOfChecks = 0;
		while (true) {

			if (isStartOfDay(numberOfChecks)) {
				numberOfChecks++;
				System.out.println("DAY: " + days);				

				//Create all instances and start them up;
				for (OnDemandAWS vm : machines)
					createAndStartUpVM(vm, bucketName);
				
				
				Thread.sleep(2*60*1000);
				//Before increasing CPU ssh needs to be initialized. This takes a around 2 minutes after the cpu starts
				System.out.println("Increase CPU");
				increaseCPU(bob, keyName);
				increaseCPU(alice, keyName);

				System.out.println("All machines are created.");	
				//Sleep for 30sec before pinging CloudWatch for the first time
				sleep(pingCW);
				continue;
			} else if (isEndOfDay(numberOfChecks)) {
				numberOfChecks = 0;
				days++;

				//Terminate all instances
				for (OnDemandAWS vm : machines)
					terminateVM(vm);
				
				if(!bob2.getIsTerminated(false))
					bob2.shutDownOnDemandAWS();
				
				//We have reached maximum number of days
				if (days > maxDays)
					break;

				System.out.println("All machines are terminated. Now SLEEP.");				
				//Sleep for the night - 2min
				sleep(nightDuration);				
				continue;
			}

			//See which machine is used a lot and try to auto-scale and which one is not used and kill it.
			numberOfChecks++;
			
			if (numberOfChecks>3){
				stopCPU(bob, keyName);
				stopCPU(alice, keyName);
			}

			//Terminate idle machines
			for (OnDemandAWS vm : machines)
				if(isIdle(cloudWatch, vm, cpuIdle)) {
					terminateVM(vm);
					System.out.println(vm.machineName + " is IDLE and terminated");
				}
			
			
			//Auto-scale code
			autoScale(bob2, getCPUUsage(cloudWatch, bob.instanceId));
			
			System.out.println("Check: " + numberOfChecks);

			//Sleep for 30sec before pinging CloudWatch again
			sleep(pingCW);
		}
		
		//Shutdown clients
		ec2.shutdown();
		s3.shutdown();
		cloudWatch.shutdown();
		
		System.out.println("EXIT Program");
	}
	
	

	private static Boolean isStartOfDay(int numberOfChecks){
		return numberOfChecks == 0;
	}

	private static Boolean isEndOfDay(int numberOfChecks){
		return numberOfChecks *pingCW > dayDuration;
	}
	
	//Checks if the CPU of a machine is idle
	private static Boolean isIdle(AmazonCloudWatchClient cloudWatch, OnDemandAWS vm, double bound) {
		if (vm.getIsTerminated(true)) return false;
		
		double p = getCPUUsage(cloudWatch, vm.instanceId);
		if (p < 0) return false;
		return p < bound;
	}
	
	//Invokes Thread sleep and handles exception
	public static void sleep(int seconds) {
		try {
			Thread.sleep(seconds*1000);
		} catch (InterruptedException e) {
			System.out.println("Caught Exception: " + e.getMessage());
		}
	}
	
	//Creates a machine and starts it up
	private static void createAndStartUpVM(OnDemandAWS machine, String bucketName) throws Exception {
		System.out.println("Creating machine...");
		machine.createInstance();			

		//Sleep before starting up
		sleep(10);

		machine.startUpOnDemandAWS();
		System.out.println("Attach EBS");
		machine.attachEBS();
		System.out.println("Attach S3");
		machine.attachS3(bucketName);
		System.out.println("Machine created.");		
	}

	//Terminates a machine and takes a snapshot
	private static void terminateVM(OnDemandAWS machine) {

		if (machine.getIsTerminated(true))
			return;

		//Try to shut down the machine	
		System.out.println("Terminating machine...");
		System.out.println("Detach EBS");
		machine.detachEBS();
		System.out.println("Take Snapshot");

		machine.saveSnapShot();		
		//wait for the snapshot to be created and then shutdown the machine
		do {
			sleep(10);
		} while(!machine.getSnapShotState().equalsIgnoreCase("available"));
		System.out.println("Snapshot created");

		System.out.println("Terminated");
		machine.shutDownOnDemandAWS();
	}

	private static void autoScale(OnDemandAWS bob2, double current) {
		Boolean isTerminated = bob2.getIsTerminated(false);
		
		if (bob2.getIsTerminated(false) && current >= upperT) {
			bob2.createInstance();
			System.out.println("Autoscale machine created.");	
		} else if (!isTerminated && current < lowerT) {
			bob2.shutDownOnDemandAWS();
			System.out.println("Autoscale machine terminated.");
		}
	}

	private static void createKey(String keyName, AmazonEC2 ec2){
		try {
			List<KeyPairInfo> keyPairList = ec2.describeKeyPairs().getKeyPairs();
			for (KeyPairInfo keyPair : keyPairList){
				if ( keyName.equalsIgnoreCase(keyPair.getKeyName()) ){
					System.out.println("Using key " + keyName);
					return;
				}
			}
			System.out.println("Creating key " + keyName + "in local directory");
			CreateKeyPairRequest newKeyRequest = new CreateKeyPairRequest();
			newKeyRequest.setKeyName(keyName);
			CreateKeyPairResult keyresult = ec2.createKeyPair(newKeyRequest);
			KeyPair keyPair = new KeyPair();
			keyPair = keyresult.getKeyPair();
			String privateKey = keyPair.getKeyMaterial();
			writeKeytoFile(keyName, privateKey);

		} catch (AmazonServiceException ase) {
			System.out.println("Caught Exception: " + ase.getMessage());
			System.out.println("Reponse Status Code: " + ase.getStatusCode());
			System.out.println("Error Code: " + ase.getErrorCode());
			System.out.println("Request ID: " + ase.getRequestId());

		}
	}

	private static void writeKeytoFile(String keyName, String privateKey) {
		FileWriter fileWriter = null;
        try {
            File keyFile = new File(keyName + ".pem");
            System.out.println("Key File written to" + System.getProperty("user.dir")); 
            keyPath = System.getProperty("user.dir"); 
            fileWriter = new FileWriter(keyFile);
            fileWriter.write(privateKey);
            fileWriter.close();
    	} catch (IOException e) {
			e.printStackTrace();
		} 
		
		
//		try {
//		
//			PrintWriter out = new PrintWriter(keyName + ".pem");
//			System.out.println("Key file at " );
//			out.println(privateKey);
//			out.close();
//		}
	
	}


	static void increaseCPU(OnDemandAWS pc, String keyName) throws InterruptedException{

		File keyfile = new File(keyName + ".pem"); // or "~/.ssh/id_dsa"
		String keyfilePass = "none"; // will be ignored if not needed

		try
		{
			Connection conn = new Connection(pc.ipAddress);
			conn.connect();

			boolean isAuthenticated = conn.authenticateWithPublicKey("ec2-user", keyfile, keyfilePass);
			if (isAuthenticated == false)
				throw new IOException("Authentication failed.");


			Session sess = conn.openSession();
			System.out.println("Increasing CPU usage for " + pc.machineName);
			sess.execCommand("while true; do true; done");
			sess.close();
			conn.close();

		}
		catch (IOException e)
		{
			e.printStackTrace(System.err);
			System.out.println("Please use the attached script to start and stop cpu remotely");
		}
	}
	
	
	static void stopCPU(OnDemandAWS pc, String keyName) throws InterruptedException{
		File keyfile = new File(keyName + ".pem"); // or "~/.ssh/id_dsa"
		String keyfilePass = "none"; // will be ignored if not needed

		try
		{
			Connection conn = new Connection(pc.ipAddress);
			conn.connect();

			boolean isAuthenticated = conn.authenticateWithPublicKey("ec2-user", keyfile, keyfilePass);
			if (isAuthenticated == false)
				throw new IOException("Authentication failed.");


			Session sess = conn.openSession();
			sess.execCommand("killall bash");
			sess.close();

			conn.close();
			
			System.out.println("Stopped CPU on " + pc.machineName);

		}
		catch (IOException e)
		{
			e.printStackTrace(System.err);
			System.out.println("Please use the attached script to start and stop cpu remotely");
		}
		
	}

	public static double getCPUUsage(AmazonCloudWatchClient cloudWatch, String instanceId){	
		
		try{
			//create request message
			GetMetricStatisticsRequest statRequest = new GetMetricStatisticsRequest();
			//set up request message
			statRequest.setNamespace("AWS/EC2"); //namespace
			statRequest.setPeriod(60); //period of data
			ArrayList<String> stats = new ArrayList<String>();
			//Use one of these strings: Average, Maximum, Minimum, SampleCount, Sum 
			stats.add("Average"); 
			stats.add("Sum");
			stats.add("Maximum");
			statRequest.setStatistics(stats);
			//Use one of these strings: CPUUtilization, NetworkIn, NetworkOut, DiskReadBytes, DiskWriteBytes, DiskReadOperations  
			statRequest.setMetricName("CPUUtilization"); 
			// set time
			GregorianCalendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
			calendar.add(GregorianCalendar.SECOND, -1 * calendar.get(GregorianCalendar.SECOND)); // 1 second ago
			Date endTime = calendar.getTime();
			calendar.add(GregorianCalendar.MINUTE, -1); // 1 minutes ago
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
			//System.out.println(statResult.toString());
			List<Datapoint> dataList = statResult.getDatapoints();
			Double averageCPU = null;
			Date timeStamp = null;
			for (Datapoint data : dataList){
				averageCPU = data.getAverage();
				timeStamp = data.getTimestamp();
				//System.out.println("Average CPU utlilization for last 1 minute since " +timeStamp.toString() + ": " +averageCPU);
				//System.out.println("Max CPU utlilization for last 1 minute since "+timeStamp.toString() + ": " + data.getMaximum());
				//System.out.println("Total CPU utlilization for last 1 minutes since "+timeStamp.toString() + ": " + data.getSum());
			}
			if (averageCPU == null){
				System.out.println("Average CPU utlilization for last 1 minute: 0" );
				return 0;
			}else{ 
				System.out.println("Average CPU utlilization for last 1 minute since " +timeStamp.toString() + ": " +averageCPU);
				return averageCPU;
			}
		}catch (AmazonServiceException ase) {
			System.out.println("Caught Exception: " + ase.getMessage());
			System.out.println("Reponse Status Code: " + ase.getStatusCode());
			System.out.println("Error Code: " + ase.getErrorCode());
			System.out.println("Request ID: " + ase.getRequestId());
			return 0;
		}
		
	}



	public static void createBucket(AmazonS3Client s3, String bucketName, String zone){
		try{

			List <Bucket> bucketList = s3.listBuckets();

			for (Bucket bucket : bucketList){
				//System.out.println(bucket.getName());
				if ( bucketName.equalsIgnoreCase(bucket.getName() )){
					System.out.println("Using bucket " + bucketName );
					return;
				}
			}

			System.out.println("Created s3 bucket " + bucketName );
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


		List <SecurityGroup> secGroupList = ec2.describeSecurityGroups().getSecurityGroups();
		for (SecurityGroup secGroup : secGroupList){
			//System.out.println(secGroup.getGroupName());
			if ( securityGroup.equalsIgnoreCase(secGroup.getGroupName()) ){
				System.out.println("Using Security Group " + securityGroup);
				return;
			}
		}

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
		//telnet
		IpPermission ipPermission5 = new IpPermission();
		ipPermission5.withIpRanges("0.0.0.0/0")
		.withIpProtocol("tcp")
		.withFromPort(23)
		.withToPort(23);

		List<IpPermission> permissions = new ArrayList<IpPermission>();
		permissions.add(ipPermission1);
		permissions.add(ipPermission2);
		permissions.add(ipPermission3);
		permissions.add(ipPermission4);
		permissions.add(ipPermission5);

		AuthorizeSecurityGroupIngressRequest authorizeSecurityGroupIngressRequest =
				new AuthorizeSecurityGroupIngressRequest();

		authorizeSecurityGroupIngressRequest.withGroupName(securityGroup)
		.withIpPermissions(permissions);

		ec2.authorizeSecurityGroupIngress(authorizeSecurityGroupIngressRequest);
		
		System.out.println("Created Security Group " + securityGroup);
	}
	
	private static void setupAutoScale(AmazonAutoScalingClient autoScale,	AmazonCloudWatchClient cloudWatch, 
			String keyName, String zone, String securityGroup, String imageId) {
		  
	
		List <LaunchConfiguration> launchList = autoScale.describeLaunchConfigurations().getLaunchConfigurations();
		for (LaunchConfiguration ll : launchList)
			if ( ll.getLaunchConfigurationName().equalsIgnoreCase("On DemandAWS") ){
				System.out.println("Using Launch Configuration " + ll.getLaunchConfigurationName());
				return;
				
			}
	    	
	
	    	CreateLaunchConfigurationRequest launchConfig = new CreateLaunchConfigurationRequest();
	    	launchConfig.setImageId(imageId);
	    	launchConfig.setKeyName(keyName);
	    	launchConfig.setInstanceType("t1.micro");
	    	 List<String> securityGroups = new ArrayList<String>();
	     	securityGroups.add(securityGroup);
	    	launchConfig.setSecurityGroups(securityGroups);
	    	launchConfig.setLaunchConfigurationName("On DemandAWS");
	    	autoScale.createLaunchConfiguration(launchConfig);
	    	
	    	
	    	CreateAutoScalingGroupRequest autoReq = new CreateAutoScalingGroupRequest();
	    	autoReq.setLaunchConfigurationName("On DemandAWS");
			List<String> availabilityZones = new ArrayList<String>();
			availabilityZones.add(zone);
			autoReq.setAvailabilityZones(availabilityZones);
			autoReq.setMinSize(1);
			autoReq.setMaxSize(1);
			autoReq.setAutoScalingGroupName("OnDemand ASGroup");
			autoScale.createAutoScalingGroup(autoReq);
			
			System.out.println("Using Launch Configuration On DemandAWS created" );
		
			
		}
	
	private static void setupPolicy(AmazonAutoScalingClient autoScale,	AmazonCloudWatchClient cloudWatch) {
		
		PutScalingPolicyRequest policyReq = new PutScalingPolicyRequest();
		policyReq.setPolicyName("On Demand Scale Up Policy");
		policyReq.setAutoScalingGroupName("OnDemand ASGroup");
		policyReq.setAdjustmentType("ChangeInCapacity");
		policyReq.setCooldown(60);
		policyReq.setScalingAdjustment(1);
		PutScalingPolicyResult arn_up = autoScale.putScalingPolicy(policyReq);
		
								
		PutMetricAlarmRequest putMetricAlarmRequest = new PutMetricAlarmRequest();
		putMetricAlarmRequest.setMetricName("HighCPUAlarm");
		putMetricAlarmRequest.setComparisonOperator("GreaterThanOrEqualToThreshold");
		putMetricAlarmRequest.setEvaluationPeriods(1);
		putMetricAlarmRequest.setMetricName("CPUUtilization");
		putMetricAlarmRequest.setNamespace("AWS/EC2");
		putMetricAlarmRequest.setPeriod(120);
		putMetricAlarmRequest.setStatistic("Average");
		putMetricAlarmRequest.setThreshold(50.0);
		List<String> arnList = new ArrayList<String>();
		arnList.add(arn_up.getPolicyARN());
		putMetricAlarmRequest.setAlarmActions(arnList);
		putMetricAlarmRequest.setAlarmName("On Demand Alarm Up");
		
		cloudWatch.putMetricAlarm(putMetricAlarmRequest);
		
		policyReq = new PutScalingPolicyRequest();
		policyReq.setPolicyName("On Demand Scale Down Policy");
		policyReq.setAutoScalingGroupName("OnDemand ASGroup");
		policyReq.setAdjustmentType("ChangeInCapacity");
		policyReq.setCooldown(60);
		policyReq.setScalingAdjustment(-1);
		PutScalingPolicyResult arn_down = autoScale.putScalingPolicy(policyReq);
		
		putMetricAlarmRequest = new PutMetricAlarmRequest();
		putMetricAlarmRequest.setMetricName("LowCPUAlarm");
		putMetricAlarmRequest.setComparisonOperator("LessThanOrEqualToThreshold");
		putMetricAlarmRequest.setEvaluationPeriods(1);
		putMetricAlarmRequest.setMetricName("CPUUtilization");
		putMetricAlarmRequest.setNamespace("AWS/EC2");
		putMetricAlarmRequest.setPeriod(120);
		putMetricAlarmRequest.setStatistic("Average");
		putMetricAlarmRequest.setThreshold(4.0);
		List<String> arnList2 = new ArrayList<String>();
		arnList2.add(arn_down.getPolicyARN());
		putMetricAlarmRequest.setAlarmActions(arnList2);
		putMetricAlarmRequest.setAlarmName("On Demand Alarm Down");
		
		
		
		cloudWatch.putMetricAlarm(putMetricAlarmRequest);
		
		System.out.println("Setup autoscaler to monitor average CPU Usage");
	}
}
