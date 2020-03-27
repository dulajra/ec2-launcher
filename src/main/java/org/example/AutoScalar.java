package org.example;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.IamInstanceProfileSpecification;
import com.amazonaws.services.ec2.model.Reservation;
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

    private static final int MAX_TOTAL_INSTANCES_ALLOWED = 8;
    private static final String INPUT_QUEUE_NAME = "inputMessageQueue";
    private static final String SECURITY_GROUP_ID = "sg-0b8881f281d0e29ff";
    private static final String IAM_ROLE_NAME = "Pi-Developer-Role";
    private static final String AMI_ID = "ami-0f288d91f39b59fa2";
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
            System.out.println("\nTotal no messages in queue: " + countOfPendingRequests);

            if (countOfPendingRequests > 0) {
                int totalInstances = numberOfRunningInstances();
                System.out.println("Total running instances: " + totalInstances);
                int workerInstances = numberOfWorkerInstances();
                System.out.println("Total running workers: " + workerInstances);

                if (countOfPendingRequests > workerInstances) {

                    int maxAllowedLaunches = MAX_TOTAL_INSTANCES_ALLOWED - totalInstances;

                    if (maxAllowedLaunches > 0) {
                        int requiredNewWorkers = countOfPendingRequests - workerInstances;
                        System.out.println("Need new workers: " + requiredNewWorkers);
                        int toCreate = Math.min(maxAllowedLaunches, requiredNewWorkers);
                        System.out.println("Creating new workers: " + toCreate);
                        createAppTierInstances(toCreate);
                    } else {
                        System.out.println("No more instances are allowed!");
                    }

                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                System.out.println("No pending requests. Going to sleep...");
                try {
                    Thread.sleep(10000);
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
        DescribeInstancesRequest request = new DescribeInstancesRequest();
        request.withFilters(
                new Filter().withName("instance-state-name").withValues("pending", "running")
        );

        DescribeInstancesResult describeInstancesResult = ec2Client.describeInstances(request);
        int count = 0;

        for (Reservation reservation : describeInstancesResult.getReservations()) {
            count += reservation.getInstances().size();
        }

        return count;
    }

    private int numberOfWorkerInstances() {
        DescribeInstancesRequest request = new DescribeInstancesRequest();
        request.withFilters(
                new Filter().withName("instance-state-name").withValues("pending", "running"),
                new Filter().withName("tag:Name").withValues("Worker Instance"),
                new Filter().withName("tag:type").withValues("auto-scaled")
        );

        DescribeInstancesResult describeInstancesResult = ec2Client.describeInstances(request);
        int count = 0;

        for (Reservation reservation : describeInstancesResult.getReservations()) {
            count += reservation.getInstances().size();
        }

        return count;
    }

    private void createAppTierInstances(int numberOfInstancesToLaunch) {
        Collection<Tag> tags = new ArrayList<>();

        Tag t = new Tag();
        t.setKey("Name");
        t.setValue("Worker Instance");
        tags.add(t);

        t = new Tag();
        t.setKey("type");
        t.setValue("auto-scaled");
        tags.add(t);

        List<String> securityGroupList = new ArrayList<String>();
        securityGroupList.add(SECURITY_GROUP_ID);

        TagSpecification tagSpecification = new TagSpecification();
        tagSpecification.setResourceType("instance");
        tagSpecification.setTags(tags);

        Collection<TagSpecification> tagSpecifications = new ArrayList<>();
        tagSpecifications.add(tagSpecification);

        IamInstanceProfileSpecification iamSpecification = new IamInstanceProfileSpecification();
        iamSpecification.setName(IAM_ROLE_NAME);

        RunInstancesRequest runRequest = new RunInstancesRequest(AMI_ID, numberOfInstancesToLaunch, numberOfInstancesToLaunch);
        runRequest.setInstanceType(INSTANCE_TYPE);
        runRequest.setSecurityGroupIds(securityGroupList);
        runRequest.setIamInstanceProfile(iamSpecification);
        runRequest.setTagSpecifications(tagSpecifications);

        RunInstancesResult runRequestResult = ec2Client.runInstances(runRequest);
    }

}