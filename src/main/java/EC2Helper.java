/*
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.CreateImageRequest;
import com.amazonaws.services.ec2.model.CreateImageResult;
import com.amazonaws.services.ec2.model.DescribeImagesRequest;
import com.amazonaws.services.ec2.model.DescribeInstanceStatusRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.TagSpecification;
import com.amazonaws.services.ec2.waiters.AmazonEC2Waiters;
import com.amazonaws.waiters.Waiter;
import com.amazonaws.waiters.WaiterParameters;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

*/
/**
 * Common helper class for EC2 related operations.
 *//*

public class EC2Helper {

    private AmazonEC2 ec2 = AmazonEC2ClientBuilder.standard().withRegion(Configs.REGION)
            .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(Configs.AWS_ACCESS_KEY_ID, Configs.AWS_SECRET_ACCESS_KEY))).build();

    */
/**
     * Launch new EC2 instance(s) with given configurations
     *
     * @param name          Name for the instance
     * @param amiID         AMI ID to be used
     * @param instanceCount Number of instances to be created
     * @return @RunInstancesResult with including details of created instances
     *//*

    public RunInstancesResult launchEC2(String name, String amiID, int instanceCount) {
        Collection<Tag> tags = new ArrayList<Tag>();
        Tag t = new Tag();
        t.setKey("Name");
        t.setValue(name);
        tags.add(t);

        TagSpecification tagSpecification = new TagSpecification();
        tagSpecification.setResourceType("instance");
        tagSpecification.setTags(tags);

        RunInstancesRequest runInstancesRequest = new RunInstancesRequest();

        runInstancesRequest.withImageId(amiID)
                .withTagSpecifications(tagSpecification)
                .withInstanceType(Configs.INSTANCE_TYPE)
                .withMinCount(instanceCount)
                .withMaxCount(instanceCount)
                .withKeyName(Configs.KEY_PAIR_NAME)
                .withSubnetId(Configs.SUBNET_ID)
                .withSecurityGroupIds(Configs.SECURITY_GROUP_ID);

        return ec2.runInstances(runInstancesRequest);
    }

    */
/**
     * Wait unit the given instance is running and all status checks are passed
     *
     * @param instance @{@link Instance} to wait for
     *//*

    public void waitUntilEC2IsReady(Instance instance) {
        Waiter<DescribeInstancesRequest> runningWaiter = new AmazonEC2Waiters(ec2).instanceRunning();
        Waiter<DescribeInstanceStatusRequest> okWaiter = new AmazonEC2Waiters(ec2).instanceStatusOk();

        DescribeInstancesRequest describeInstancesRequest = new DescribeInstancesRequest();
        describeInstancesRequest.setInstanceIds(Collections.singletonList(instance.getInstanceId()));

        DescribeInstanceStatusRequest describeInstanceStatusRequest = new DescribeInstanceStatusRequest();
        describeInstanceStatusRequest.setInstanceIds(Collections.singletonList(instance.getInstanceId()));

        System.out.println(MessageFormat.format("Waiting until instance {0} is running...", instance.getInstanceId()));
        runningWaiter.run(new WaiterParameters<DescribeInstancesRequest>().withRequest(describeInstancesRequest));
        System.out.println(MessageFormat.format("Waiting until instance {0} is ready...", instance.getInstanceId()));
        okWaiter.run(new WaiterParameters<DescribeInstanceStatusRequest>().withRequest(describeInstanceStatusRequest));

        System.out.println(MessageFormat.format("Instance {0} is ready to use", instance.getInstanceId()));
    }

    */
/**
     * Get public IP address of the given EC2 instance ID
     *
     * @param instanceID EC2 instance ID
     * @return Public IP address of the instance
     *//*

    public String getPublicIPOfInstance(String instanceID) {
        DescribeInstancesRequest request = new DescribeInstancesRequest().withInstanceIds(instanceID);
        DescribeInstancesResult result = ec2.describeInstances(request);
        List<Reservation> reservations = result.getReservations();

        if (reservations.get(0).getInstances().isEmpty()) {
            System.out.println("No instances found for the given ID");
            return null;
        } else {
            return reservations.get(0).getInstances().get(0).getPublicIpAddress();
        }
    }

    */
/**
     * Get instance ID address of the given EC2 instance with given public IP
     *
     * @param publicIP Public IP of EC2 instance
     * @return Instance ID
     *//*

    public String getInstanceID(String publicIP) {
        DescribeInstancesRequest request = new DescribeInstancesRequest().withFilters(new Filter("ip-address").withValues(publicIP));
        DescribeInstancesResult result = ec2.describeInstances(request);
        List<Reservation> reservations = result.getReservations();

        if (reservations.get(0).getInstances().isEmpty()) {
            System.out.println("No instances found for the given public IP");
            return null;
        } else {
            String instanceId = reservations.get(0).getInstances().get(0).getInstanceId();
            System.out.println(MessageFormat.format("Instance {0} found for public IP {1}", instanceId, publicIP));
            return instanceId;
        }
    }

    */
/**
     * Create a new AMI using the given EC2 instance. This function will wait until the AMI reaches to available state
     *
     * @param instanceID  ID of the EC2 instance to be used for AMI
     * @param description Description for newly created AMI
     * @return ID of the created AMI
     *//*

    public String createAMI(String instanceID, String description) {
        CreateImageResult imageResult = ec2.createImage(new CreateImageRequest()
                .withInstanceId(instanceID)
                .withDescription(description)
                .withName(description + "-" + System.currentTimeMillis())
                .withNoReboot(false));

        String amiId = imageResult.getImageId();

        Waiter<DescribeImagesRequest> waiter = new AmazonEC2Waiters(ec2).imageAvailable();

        DescribeImagesRequest describeImagesRequest = new DescribeImagesRequest();
        describeImagesRequest.setImageIds(Collections.singletonList(amiId));

        System.out.println(MessageFormat.format("Waiting until AMI {0} is available...", amiId));
        waiter.run(new WaiterParameters<DescribeImagesRequest>().withRequest(describeImagesRequest));
        System.out.println(MessageFormat.format("AMI {0} is available to use", amiId));

        return amiId;
    }
}
*/
