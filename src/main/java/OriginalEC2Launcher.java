import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import java.text.MessageFormat;
import java.util.List;

public class OriginalEC2Launcher {

    private EC2Helper ec2Helper = new EC2Helper();

    public String launch(String name) {
        RunInstancesResult runInstancesResult = ec2Helper.launchEC2(name, Configs.ORIGINAL_INSTANCE_AMI_ID, 1);

        if (runInstancesResult.getReservation().getInstances().isEmpty()) {
            System.out.println("EC2 not launched. No reservations found");
            return null;
        } else {
            Instance instance = runInstancesResult.getReservation().getInstances().get(0);
            ec2Helper.waitUntilEC2IsReady(instance);
            String publicIP = ec2Helper.getPublicIPOfInstance(instance.getInstanceId());

            System.out.println(MessageFormat.format("Original instance launched. Instance ID: {0}, Public IP: {1}", instance.getInstanceId(), publicIP));

            copyJarToInstance(publicIP, Configs.INSTANCE_USERNAME, Configs.KEY_FILE_PATH, Configs.JAR_FILE_PATH, Configs.REMOTE_JAR_DIRECTORY);
            setCronConfig(publicIP, Configs.INSTANCE_USERNAME, Configs.KEY_FILE_PATH);
            return publicIP;
        }
    }

    private void copyJarToInstance(String instanceIp, String username, String keyFilePath, String localFilePath, String remoteDirectory) {
        Session session = SSHHelper.createSession(username, instanceIp, 22, keyFilePath, null);
        System.out.println("SCP session established successfully");

        SSHHelper.scp(session, localFilePath, remoteDirectory);
        System.out.println("File copied to remote successfully");
    }

    public void setCronConfig(String instanceIp, String username, String keyFilePath) {
        Session session = SSHHelper.createSession(username, instanceIp, 22, keyFilePath, null);
        List<String> ls = SSHHelper.sshAndRunCommand(session, MessageFormat.format("crontab -l > mycron; echo \"{0}\" >> mycron; crontab mycron; rm mycron", Configs.CRON_COMMAND));
    }
}
