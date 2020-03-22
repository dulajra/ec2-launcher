import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;

public class OriginalEC2Launcher {

    private EC2Helper ec2Helper = new EC2Helper();

    private static Session createSession(String user, String host, int port, String keyFilePath, String keyPassword) {
        try {
            JSch jsch = new JSch();

            if (keyFilePath != null) {
                if (keyPassword != null) {
                    jsch.addIdentity(keyFilePath, keyPassword);
                } else {
                    jsch.addIdentity(keyFilePath);
                }
            }

            Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");

            Session session = jsch.getSession(user, host, port);
            session.setConfig(config);
            session.connect();

            return session;
        } catch (JSchException e) {
            System.out.println(e);
            return null;
        }
    }

    private static List<String> runCommand(Session session, String command) throws JSchException {
        ChannelExec channel = (ChannelExec) session.openChannel("exec");

        try {
            channel.setCommand(command);
            channel.setInputStream(null);
            InputStream output = channel.getInputStream();
            channel.connect();

//            String result = CharStreams.toString(new InputStreamReader(output));
//            return asList(result.split("\n"));

            String result = new BufferedReader(new InputStreamReader(output))
                    .lines().collect(Collectors.joining("\n"));
            return asList(result.split("\n"));

        } catch (JSchException | IOException e) {
            closeConnection(channel, session);
            throw new RuntimeException(e);

        } finally {
            closeConnection(channel, session);
        }
    }

    private static void closeConnection(ChannelExec channel, Session session) {
        try {
            channel.disconnect();
        } catch (Exception ignored) {
        }
        session.disconnect();
    }

    private static void copyLocalToRemote(Session session, String localFilePath, String remoteDirectory) throws JSchException, IOException {
        boolean ptimestamp = true;

        // exec 'scp -t rfile' remotely
        String command = "scp " + (ptimestamp ? "-p" : "") + " -t " + remoteDirectory;
        Channel channel = session.openChannel("exec");
        ((ChannelExec) channel).setCommand(command);

        // get I/O streams for remote scp
        OutputStream out = channel.getOutputStream();
        InputStream in = channel.getInputStream();

        channel.connect();

        if (checkAck(in) != 0) {
            System.exit(0);
        }

        File _lfile = new File(localFilePath);

        if (ptimestamp) {
            command = "T" + (_lfile.lastModified() / 1000) + " 0";
            // The access time should be sent here,
            // but it is not accessible with JavaAPI ;-<
            command += (" " + (_lfile.lastModified() / 1000) + " 0\n");
            out.write(command.getBytes());
            out.flush();
            if (checkAck(in) != 0) {
                System.exit(0);
            }
        }

        // send "C0644 filesize filename", where filename should not include '/'
        long filesize = _lfile.length();
        command = "C0644 " + filesize + " ";
        if (localFilePath.lastIndexOf('/') > 0) {
            command += localFilePath.substring(localFilePath.lastIndexOf('/') + 1);
        } else {
            command += localFilePath;
        }

        command += "\n";
        out.write(command.getBytes());
        out.flush();

        if (checkAck(in) != 0) {
            System.exit(0);
        }

        // send a content of lfile
        FileInputStream fis = new FileInputStream(localFilePath);
        byte[] buf = new byte[1024];
        while (true) {
            int len = fis.read(buf, 0, buf.length);
            if (len <= 0) break;
            out.write(buf, 0, len); //out.flush();
        }

        // send '\0'
        buf[0] = 0;
        out.write(buf, 0, 1);
        out.flush();

        if (checkAck(in) != 0) {
            System.exit(0);
        }
        out.close();

        try {
            if (fis != null) fis.close();
        } catch (Exception ex) {
            System.out.println(ex);
        }

        channel.disconnect();
        session.disconnect();
    }

    private static int checkAck(InputStream in) throws IOException {
        int b = in.read();
        // b may be 0 for success,
        //          1 for error,
        //          2 for fatal error,
        //         -1
        if (b == 0) return b;
        if (b == -1) return b;

        if (b == 1 || b == 2) {
            StringBuffer sb = new StringBuffer();
            int c;
            do {
                c = in.read();
                sb.append((char) c);
            }
            while (c != '\n');
            if (b == 1) { // error
                System.out.print(sb.toString());
            }
            if (b == 2) { // fatal error
                System.out.print(sb.toString());
            }
        }
        return b;
    }

    private static void copyJarToInstance(String instanceIp, String username, String keyFilePath, String localFilePath, String remoteDirectory) {
        Session session = createSession(username, instanceIp, 22, keyFilePath, null);
        System.out.println("SCP session established successfully");

        try {
            copyLocalToRemote(session, localFilePath, remoteDirectory);
            System.out.println("File copied to remote successfully");
        } catch (JSchException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

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

    public static void setCronConfig (String instanceIp, String username, String keyFilePath) {
        Session session = createSession(username, instanceIp, 22, keyFilePath, null);

        try {
            List<String> ls = runCommand(session, MessageFormat.format("crontab -l > mycron; echo \"{0}\" >> mycron; crontab mycron; rm mycron", Configs.CRON_COMMAND));
            System.out.println("done");
        } catch (JSchException e) {
            e.printStackTrace();
        }
    }
}
