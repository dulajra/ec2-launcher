import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import java.text.MessageFormat;
import java.util.List;

/**
 * All operations related to original EC2 instance
 */
public class OriginalEC2Launcher {

    private EC2Helper ec2Helper = new EC2Helper();

    /**
     * Launch an original instance with the given name
     *
     * @param name Name for the new instance
     * @return Public IP address of the created instance
     */
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

    /**
     * Copy the Jar file from local machine to remote instance using SCP
     *
     * @param instanceIp IP of remote instance
     * @param username Username of EC2 instance to log in
     * @param keyFilePath Absolute path to key file (pem file)
     * @param localFilePath Absolute path of local jar file
     * @param remoteDirectory Path of the remote instance directory to copy
     */
    private void copyJarToInstance(String instanceIp, String username, String keyFilePath, String localFilePath, String remoteDirectory) {
        Session session = SSHHelper.createSession(username, instanceIp, 22, keyFilePath, null);
        System.out.println("SCP session established successfully");

        System.out.println("Copying Jar file to remote instance");
        SSHHelper.scp(session, localFilePath, remoteDirectory);
        System.out.println("File copied to remote successfully");
    }

    /**
     * Set cron configurations on the remote instance
     *
     * @param instanceIp IP of remote instance
     * @param username Username of EC2 instance to log in
     * @param keyFilePath Absolute path to key file (pem file)
     */
    public void setCronConfig(String instanceIp, String username, String keyFilePath) {
        Session session = SSHHelper.createSession(username, instanceIp, 22, keyFilePath, null);
        System.out.println("SSH session established successfully");

        String command = MessageFormat.format("crontab -l > mycron; echo \"{0}\" >> mycron; crontab mycron; rm mycron", Configs.CRON_COMMAND);
        List<String> result = SSHHelper.sshAndRunCommand(session, command);
        System.out.println("Cron configurations set successfully");
    }
}
