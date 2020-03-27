package org.example;

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
import main.java.Configs;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Worker {

    private static final String INPUT_S3_NAME = "ccjarfiles";
    private static final String OUTPUT_S3_NAME = "ccjarfiles";
    private static final String INPUT_QUEUE_NAME = "inputMessageQueue";
    private static final String OUTPUT_QUEUE_NAME = "outputMessageQueue";

    private static final String INPUT_FILE_DOWNLOAD_PATH = "/home/ubuntu/resources/input/";
    private static final String OUTPUT_RESULT_PATH = "/home/ubuntu/resources/output/";
    private static final String DARKNET_PATH = "/home/ubuntu/darknet/";

    private static final String COMMAND = "Xvfb :1 & export DISPLAY=:1;cd {0};./darknet detector demo cfg/coco.data cfg/yolov3-tiny.cfg yolov3-tiny.weights {1}{3} -dont_show > {2}{3}.txt";

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

    private static String executeCommand(String command) {
        StringBuffer recognisedImage = new StringBuffer();

        Process imageRecognitionReq;
        try {
            imageRecognitionReq = Runtime.getRuntime().exec(command);
            imageRecognitionReq.waitFor();
            BufferedReader terminalReader = new BufferedReader(new InputStreamReader(imageRecognitionReq.getInputStream()));

            String eachLine = "";
            while ((eachLine = terminalReader.readLine()) != null) {
                recognisedImage.append(eachLine + "\n");
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "-1";
        }
        return recognisedImage.toString();
    }

    public void run() {
        while (true) {
            String key = getKeyFromSQS();

            if (key != null) {
                boolean fileDownloadStatus = downloadFileFromS3(key);

                if (fileDownloadStatus) {
                    recognizeObject(key);
                    List<String> results = extractOutput(OUTPUT_RESULT_PATH + key);
                    StringBuilder outputKeyBuilder = new StringBuilder("(\"")
                            .append(key)
                            .append("\",\"");
                    results.forEach(s -> outputKeyBuilder.append(s).append(","));
                    outputKeyBuilder.append("\")");
                    System.out.println(outputQueueUrl.toString());
                }
            } else {
                System.out.println("Sleeping for 5 seconds...\n");
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
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

    private String getKeyFromSQS() {
        ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(inputQueueUrl);
        receiveMessageRequest.setMaxNumberOfMessages(1);
        receiveMessageRequest.setVisibilityTimeout(90);
        receiveMessageRequest.setWaitTimeSeconds(0);
        ReceiveMessageResult result = sqsClient.receiveMessage(receiveMessageRequest);

        System.out.println("Checking for new messages");
        List<Message> messages = result.getMessages();

        if (messages.isEmpty()) {
            System.out.println("No new messages found!");
            return null;
        } else {
            String key = messages.get(0).getBody();
            System.out.println("New message found. Key: " + key);
            return key;
        }
    }

    private void recognizeObject(String key) {
        System.out.println("Start recognition...");

        try {
            ProcessBuilder pb = new ProcessBuilder(MessageFormat.format(COMMAND, DARKNET_PATH, INPUT_FILE_DOWNLOAD_PATH, OUTPUT_RESULT_PATH, key));
            Process process = pb.start();
            int errCode = process.waitFor();
            System.out.println("Exited with code : " + errCode);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<String> extractOutput(String filepath) {
        Set<String> results = new HashSet<>();

        try {
            String fileContent = new String(Files.readAllBytes(Paths.get(filepath)), StandardCharsets.UTF_8);
            String[] lines = fileContent.split("\n");

            for (String line : lines) {
                if (line.contains(":") && line.contains("%")) {
                    results.add(line.split(":")[0]);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new ArrayList<>(results);
    }

}
