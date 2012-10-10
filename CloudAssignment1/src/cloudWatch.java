import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.Datapoint;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsRequest;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsResult;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.s3.AmazonS3Client;


public class cloudWatch {

	
	 static AmazonEC2      ec2;
	    static AmazonS3Client s3;

	    public static void main(String[] args) throws Exception {


	    	 AWSCredentials credentials = new PropertiesCredentials(
	    			 	awsStartup.class.getResourceAsStream("AwsCredentials.properties"));
	    
	    	// we assume that we've already created an instance. Use the id of the instance.
	         	String instanceId = "i-fc2c6681"; //put your own instance id to test this code.
	         	
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
	    }
}
