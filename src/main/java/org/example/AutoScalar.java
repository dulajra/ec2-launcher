package org.example;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.DescribeInstanceStatusRequest;
import com.amazonaws.services.ec2.model.InstanceStateName;
import com.amazonaws.services.ec2.model.InstanceStatus;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.TagSpecification;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.GetQueueAttributesRequest;
import main.java.Configs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class AutoScalar {

    private static final int MAX_TOTAL_INSTANCES_ALLOWED = 5;
    private static final String INPUT_QUEUE_NAME = "inputMessageQueue";
    private static final String SECURITY_GROUP_ID = "sg-0b8881f281d0e29ff";
    private static final String AMI_ID = "";
    private static final String INSTANCE_TYPE = "t2.micro";

    private AmazonSQS sqsClient = AmazonSQSClientBuilder.standard()
            .withRegion(Configs.REGION)
            .build();
    private AmazonEC2 ec2Client = AmazonEC2ClientBuilder.standard()
            .withRegion(Configs.REGION)
            .build();
    private String inputQueueUrl = sqsClient.getQueueUrl(INPUT_QUEUE_NAME).getQueueUrl();

    public void run() {
        scaleUpInstances();
    }

    public void scaleUpInstances() {
        while (true) {
            int countOfPendingRequests = numberOfMessagesCurrentlyInQueue(inputQueueUrl);

            if (countOfPendingRequests > 0) {

                int alreadyRunningInstances = numberOfRunningInstances();

                int numberOfAppTierInstances = alreadyRunningInstances - 1;

                if (countOfPendingRequests > numberOfAppTierInstances) {

                    int MaximumAppInstances = MAX_TOTAL_INSTANCES_ALLOWED - numberOfAppTierInstances;

                    if (MaximumAppInstances > 0) {
                        int temp1 = countOfPendingRequests - numberOfAppTierInstances;
                        int toCreate = Math.min(MaximumAppInstances, temp1);
                        createAppTierInstances(toCreate);
                    }

                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        }
    }

    private int numberOfMessagesCurrentlyInQueue(String inputQueueURL) {
        List<String> attributes = new ArrayList<String>();
        attributes.add("ApproximateNumberOfMessages");
        GetQueueAttributesRequest getQueueAttributesRequest = new GetQueueAttributesRequest(inputQueueURL, attributes);
        Map<String, String> response_map = sqsClient.getQueueAttributes(getQueueAttributesRequest).getAttributes();
        return Integer.parseInt(response_map.get("ApproximateNumberOfMessages"));
    }

    private int numberOfRunningInstances() {
        DescribeInstanceStatusRequest request = new DescribeInstanceStatusRequest();
        request.setIncludeAllInstances(true);

        List<InstanceStatus> instanceList = ec2Client.describeInstanceStatus(request).getInstanceStatuses();
        int count = 0;

        for (InstanceStatus i : instanceList) {
            if (i.getInstanceState().getName().equals(InstanceStateName.Running.toString()) || i.getInstanceState().getName().equals(InstanceStateName.Pending.toString())) {
                count += 1;
            }
        }

        return count;
    }

    private void createAppTierInstances(int numberOfInstancesToLaunch) {
        Collection<Tag> tags = new ArrayList<>();
        Tag t = new Tag();
        t.setKey("Name");
        t.setValue("Worker-Instance");
        tags.add(t);

        List<String> securityGroupList = new ArrayList<String>();
        securityGroupList.add(SECURITY_GROUP_ID);

        TagSpecification tagSpecification = new TagSpecification();
        tagSpecification.setResourceType("instance");
        tagSpecification.setTags(tags);

        Collection<TagSpecification> tagSpecifications = new ArrayList<>();
        tagSpecifications.add(tagSpecification);

        RunInstancesRequest runRequest = new RunInstancesRequest(AMI_ID, numberOfInstancesToLaunch, numberOfInstancesToLaunch);
        runRequest.setInstanceType(INSTANCE_TYPE);
        runRequest.setSecurityGroupIds(securityGroupList);
        runRequest.setTagSpecifications(tagSpecifications);

        RunInstancesResult runRequestResult = ec2Client.runInstances(runRequest);
    }

}