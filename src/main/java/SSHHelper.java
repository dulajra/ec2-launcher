/*
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.sun.istack.internal.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;

public class SSHHelper {

    */
/**
     * Open a SSH session with the remote server
     *
     * @param user        Linux user to log it
     * @param host        IP Address of the remote instance
     * @param port        Port to be used for SSH/SCP
     * @param keyFilePath Absolute path of the key file (pem file)
     * @param keyPassword Passphrase for the ky file if any else null
     * @return A new SSH @{@link Session}
     *//*

    public static @Nullable
    Session createSession(String user, String host, int port, String keyFilePath, String keyPassword) {
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
            System.err.println(e);
            return null;
        }
    }

    */
/**
     * Execute the given command on a remote host using SSH
     *
     * @param session SSH Session to connect with remote host
     * @param command Command to be executed on remote host
     * @return stdout response of the remote host command execution
     *//*

    public static @Nullable
    List<String> sshAndRunCommand(Session session, String command) {
        ChannelExec channel = null;
        try {
            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);
            channel.setInputStream(null);
            InputStream output = channel.getInputStream();
            channel.connect();

            String result = new BufferedReader(new InputStreamReader(output))
                    .lines().collect(Collectors.joining("\n"));
            return asList(result.split("\n"));

        } catch (JSchException | IOException e) {
            System.err.println(e);
            return null;
        } finally {
            closeConnection(channel, session);
        }
    }

    */
/**
     * Copy a file from local machine to a remote host using SCP
     *
     * @param session         SSH session to connect with remote host
     * @param localFilePath   Absolute path of the local file
     * @param remoteDirectory Destination directory to copy the file in remote host
     *//*

    public static void scp(Session session, String localFilePath, String remoteDirectory) {
        boolean ptimestamp = true;
        ChannelExec channel = null;

        try {
            // exec 'scp -t rfile' remotely
            String command = "scp " + (ptimestamp ? "-p" : "") + " -t " + remoteDirectory;
            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);

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
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSchException e) {
            e.printStackTrace();
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
}
*/
