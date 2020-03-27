package org.example;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.*;

public class WorkerTest {

    @Test
    public void test () {
        extractOutput("/home/dulaj/workspace/PERSONAL/ec2-launcher/src/test/resources/output.txt");
    }

    public void extractOutput (String filepath) {
//        String result_label = executeCommand(MessageFormat.format("cat {0}", filepath));
//        System.out.println(result_label);



//        try {
//            List<String> lines = Files.lines(Paths.get(filepath)).collect(Collectors.toList());
//            System.out.println("");
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        String[] split = result_label.split("\\s+");

        String result_label = null;
        try {
            result_label = new String(Files.readAllBytes(Paths.get(filepath)), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
        String[] split = result_label.split("\n");
        Set<String> need = new HashSet<>();
        for (String s :
                split) {
            if(s.contains(":") && s.contains("%")) {
                need.add(s.split(":")[0]);
            }
        }

        System.out.println("");
    }

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

}