package main.java;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.amazonaws.util.EC2MetadataUtils;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.List;

public class Worker {

    private static final String INPUT_S3_NAME = "ccjarfiles";
    private static final String OUTPUT_S3_NAME = "ccjarfiles";
    private static final String INPUT_QUEUE_NAME = "inputMessageQueue";
    private static final String OUTPUT_QUEUE_NAME = "outputMessageQueue";

    private static final String INPUT_FILE_DOWNLOAD_PATH = "./resources/input/";
    private static final String OUTPUT_RESULT_PATH = "./resources/output/";
    private static final String DARKNET_PATH = "./code/darknet/";

    private static final String COMMAND = "bash -c Xvfb :1 & export DISPLAY=:1; cd {0}; ./darknet detector demo cfg/coco.data cfg/yolov3-tiny.cfg yolov3-tiny.weights {1}{2} -dont_show > {3};";

    private AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
            .withRegion(Configs.REGION)
            .build();

    private AmazonSQS sqsClient = AmazonSQSClientBuilder.standard()
            .withRegion(Configs.REGION)
            .build();

    private AmazonEC2 ec2Client = AmazonEC2ClientBuilder.standard()
            .withRegion(Configs.REGION)
            .build();

    private String inputQueueUrl = sqsClient.getQueueUrl(INPUT_QUEUE_NAME).getQueueUrl();
    private String outputQueueUrl = sqsClient.getQueueUrl(OUTPUT_QUEUE_NAME).getQueueUrl();

    public void run() {
        String key = getKeyFromSQS();
        boolean fileDownloadStatus = downloadFileFromS3(key);

        if (fileDownloadStatus) {
            recognizeObject(key);
        }
    }

    private String getKeyFromSQS() {
        ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(inputQueueUrl);
        receiveMessageRequest.setMaxNumberOfMessages(1);
        receiveMessageRequest.setVisibilityTimeout(90);
        receiveMessageRequest.setWaitTimeSeconds(0);
        ReceiveMessageResult result = sqsClient.receiveMessage(receiveMessageRequest);

        System.out.println("Checking for new messages");
        List<Message> messages = result.getMessages();
        String key = null;

        if (messages.isEmpty()) {
            System.out.println("No new messages found!");
        } else {
            key = messages.get(0).getBody();
            System.out.println("New message found. Key: " + key);
        }

        return key;
    }

    private boolean downloadFileFromS3(String key) {
        S3Object o = s3Client.getObject(INPUT_S3_NAME, key);
        S3ObjectInputStream s3is = o.getObjectContent();
        boolean result = false;

        String saveFilePath = INPUT_FILE_DOWNLOAD_PATH + key;
        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(saveFilePath);
            int bytesRead = -1;
            byte[] buffer = new byte[2048];

            System.out.println("Downloading file from S3. Filename: " + key);
            while ((bytesRead = s3is.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            System.out.println("File downloaded successfully");
            result = true;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (outputStream != null) {
                    outputStream.close();
                }
                s3is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return result;
    }

    private void terminateInstance() {
        System.out.println("Terminating the instance itself...");
        String myId = EC2MetadataUtils.getInstanceId();
        TerminateInstancesRequest request = new TerminateInstancesRequest().withInstanceIds(myId);
        ec2Client.terminateInstances(request);
    }

    private void recognizeObject(String key) {
        System.out.println("Start recognition...");

        try {
            ProcessBuilder pb = new ProcessBuilder(MessageFormat.format(COMMAND, DARKNET_PATH, INPUT_FILE_DOWNLOAD_PATH, key, OUTPUT_RESULT_PATH));
            Process process = pb.start();
            int errCode = process.waitFor();
            System.out.println("Exited with code : " + errCode);
        } catch (Exception e) {
            e.printStackTrace();
        }

        StringBuilder recognisedImage = new StringBuilder();
        String result = "-1";
        Process imageRecognitionReq;

        try {
            imageRecognitionReq = Runtime.getRuntime().exec("cat darknet/result_label");
            imageRecognitionReq.waitFor();
            BufferedReader terminalReader = new BufferedReader(new InputStreamReader(imageRecognitionReq.getInputStream()));

            String eachLine = "";
            while ((eachLine = terminalReader.readLine()) != null) {
                recognisedImage.append(eachLine).append("\n");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        result = recognisedImage.toString();
        System.out.println("Recognition complete. Result: " + result);
    }

}
