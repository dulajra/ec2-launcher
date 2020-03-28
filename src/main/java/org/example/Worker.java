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

import java.io.BufferedReader;
import java.io.File;
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

    private static final int MAX_SLEEPS_BEFORE_SHUTDOWN = 3;

    private static final String INPUT_S3_NAME = "ccjarfiles";
    private static final String OUTPUT_S3_NAME = "ccjarfiles";
    private static final String INPUT_QUEUE_NAME = "inputMessageQueue";
    private static final String OUTPUT_QUEUE_NAME = "outputMessageQueue";

    private static final String INPUT_FILE_DOWNLOAD_PATH = "/home/ubuntu/resources/input/";
    private static final String OUTPUT_RESULT_PATH = "/home/ubuntu/resources/output/";
    private static final String DARKNET_PATH = "/home/ubuntu/darknet/";

    private static final String COMMAND = "Xvfb :1 & export DISPLAY=:1;cd {0};./darknet detector demo cfg/coco.data cfg/yolov3-tiny.cfg yolov3-tiny.weights {1}{3} -dont_show > {2}{3}.txt";

    private AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
            .withRegion(AWSConfigs.REGION)
            .build();

    private AmazonSQS sqsClient = AmazonSQSClientBuilder.standard()
            .withRegion(AWSConfigs.REGION)
            .build();

    private AmazonEC2 ec2Client = AmazonEC2ClientBuilder.standard()
            .withRegion(AWSConfigs.REGION)
            .build();

    private String inputQueueUrl = sqsClient.getQueueUrl(INPUT_QUEUE_NAME).getQueueUrl();
    private String outputQueueUrl = sqsClient.getQueueUrl(OUTPUT_QUEUE_NAME).getQueueUrl();

    private Message message;
    private int sleepsCount = 0;

    public void run() {
        while (true) {
            String key = getKeyFromSQS();

            if (key != null) {
                boolean fileDownloadStatus = downloadFileFromS3(key);

                if (fileDownloadStatus) {
                    recognizeObject(key);

                    List<String> results = extractOutput(OUTPUT_RESULT_PATH + key + ".txt");
                    StringBuilder resultLabelBuilder = new StringBuilder();

                    for (int i = 0; i < results.size(); i++) {
                        resultLabelBuilder.append(results.get(i));

                        if (i < results.size() - 1) {
                            resultLabelBuilder.append(",");
                        }
                    }

                    String resultLabel = resultLabelBuilder.toString();
                    System.out.println("Recognition result: " + resultLabel);

                    String outputFilename = "(" + key + "," + resultLabel + ")";
                    s3Client.putObject(OUTPUT_S3_NAME, outputFilename, new File(OUTPUT_RESULT_PATH + key + ".txt"));

                    System.out.println("Publishing result to SQS...");
                    sqsClient.deleteMessage(inputQueueUrl, message.getReceiptHandle());
                    System.out.println("Uploading result to S3...");
                    sqsClient.sendMessage(outputQueueUrl, outputFilename);

                    System.out.println("Cleaning worker...");
                    executeCommand("rm -rf " + INPUT_FILE_DOWNLOAD_PATH + key);
                    executeCommand("rm -rf " + OUTPUT_RESULT_PATH + outputFilename);
                }
            } else {
                if (sleepsCount++ < MAX_SLEEPS_BEFORE_SHUTDOWN) {
                    System.out.println("Sleeping for 5 seconds. Sleep count: " + sleepsCount + "\n");
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    System.out.println("Max sleeps reached. Terminating the worker");
                    terminateInstance();
                    break;
                }
            }
        }
    }

    /**
     * Run the given linux command
     * @param command Command to run
     * @return Stdout of the command run result
     */
    private String executeCommand(String command) {
        StringBuilder stdout = new StringBuilder();

        Process imageRecognitionReq;
        try {
            imageRecognitionReq = Runtime.getRuntime().exec(command);
            imageRecognitionReq.waitFor();
            BufferedReader terminalReader = new BufferedReader(new InputStreamReader(imageRecognitionReq.getInputStream()));

            String eachLine = "";
            while ((eachLine = terminalReader.readLine()) != null) {
                stdout.append(eachLine).append("\n");
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "-1";
        }

        return stdout.toString();
    }

    /**
     * Get one key from SQS
     * @return Key
     */
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
            message = messages.get(0);
            String key = message.getBody();
            System.out.println("New message found. Key: " + key);
            return key;
        }
    }

    /**
     * Download fie with the given key from S3 to local machine
     * @param key
     * @return Download status.If success true else false
     */
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

    /**
     * Initiate a EC2 terminate request itself
     */
    private void terminateInstance() {
        System.out.println("Terminating the instance itself...");
        String myId = EC2MetadataUtils.getInstanceId();
        TerminateInstancesRequest request = new TerminateInstancesRequest().withInstanceIds(myId);
        ec2Client.terminateInstances(request);
    }

    /**
     * Run darknet on a video and write output to a file
     * @param inputFileName Name of the inout video file
     */
    private void recognizeObject(String inputFileName) {
        System.out.println("Start recognition...");

        try {
            ProcessBuilder pb = new ProcessBuilder("bash", "-c", MessageFormat.format(COMMAND, DARKNET_PATH, INPUT_FILE_DOWNLOAD_PATH, OUTPUT_RESULT_PATH, inputFileName));
            Process process = pb.start();
            int errCode = process.waitFor();
            System.out.println("Recognition complete. Exited with code : " + errCode);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Extract the detection results from the file generated by @recognizeObject
     * @param filepath Path of the file to extract information
     * @return Results
     */
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
