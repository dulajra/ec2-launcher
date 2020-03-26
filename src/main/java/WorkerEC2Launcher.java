/*
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.RunInstancesResult;

import java.text.MessageFormat;

public class WorkerEC2Launcher {

    private EC2Helper ec2Helper = new EC2Helper();

    */
/**
     * Launch an worker instance with the given name
     *
     * @param name  Name for the new instance
     * @param amiID AMI ID to be used
     * @return Public IP address of the created instance
     *//*

    public String launch(String name, String amiID) {
        RunInstancesResult runInstancesResult = ec2Helper.launchEC2(name, amiID, 1);

        if (runInstancesResult.getReservation().getInstances().isEmpty()) {
            System.out.println("EC2 not launched. No reservations found");
            return null;
        } else {
            Instance instance = runInstancesResult.getReservation().getInstances().get(0);
            ec2Helper.waitUntilEC2IsReady(instance);
            String publicIP = ec2Helper.getPublicIPOfInstance(instance.getInstanceId());

            System.out.println(MessageFormat.format("Worker instance launched. Instance ID: {0}, Public IP: {1}", instance.getInstanceId(), publicIP));
            return publicIP;
        }
    }

    */
/**
     * Clone an EC2 instance
     *
     * @param name     Name for the new instance
     * @param publicIP Public IP of the EC2 instance ID to be used as source
     * @return Instance ID of the cloned instance
     *//*

    public String clone(String name, String publicIP) {
        String sourceInstanceID = ec2Helper.getInstanceID(publicIP);
        String amiID = ec2Helper.createAMI(sourceInstanceID, sourceInstanceID);
        return launch(name, amiID);
    }
}
*/
