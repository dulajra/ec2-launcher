import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.RunInstancesResult;

import java.text.MessageFormat;

public class EC2Cloner {

    private EC2Helper ec2Helper = new EC2Helper();

    /**
     * Launch an worker instance with the given name
     *
     * @param name Name for the new instance
     * @return Public IP address of the created instance
     */
    public String clone (String name) {
        RunInstancesResult runInstancesResult = ec2Helper.launchEC2(name, Configs.WORKER_INSTANCE_AMI_ID, 1);

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
}
