import java.lang.System;
import java.io.IOException;
import java.io.*;
import java.net.*;
//
// This is an implementation of a simplified version of a command
// line ftp client. The program always takes two arguments
//


public class BasicFTP
{
    static final int MAX_LEN = 255;
    static final int ARG_CNT = 2;
    static final int ARG_NO_PORT = 1;

    // resources
    static Socket socket = new Socket();

    static PrintWriter clientOut;
    static BufferedReader ftpIn;
    static BufferedReader clientIn;


    // Returns a String array containing each substring from str
    private static String[] splitString(String str){
        return str.split("\\s+");
    }

    
    /**
     * Handles the first response from the server and prints it.
     */
    private static void handleFirst(){
        String fromServer;
        try {
            if ((fromServer = ftpIn.readLine()) != null) {
                System.out.println("<-- " + fromServer);
            }
        } catch (IOException e) {
            System.out.println("0xFFFD Control connection I/O error, closing control connection.");
            System.exit(0);
        }
    }

    /**
     * Takes the server response from the ftpIn BufferedReader and prints it.
     */
    private static void handleServerResponse(){
        String fromServer;
        try {
            if ((fromServer = ftpIn.readLine()) != null) {
                // check if there is a multi-line response: if there is a dash right after the response code or
                // just check if the space-splitted array's first element has more than 3 chars
                // RFC 959 for more info: https://www.ietf.org/rfc/rfc959.txt
                String[] firstLine = fromServer.split(" ");
                if (firstLine[0].length() != 3){ // it is a multi-line response
                    String responseCode = fromServer.split("-")[0];
                    System.out.println("<-- " + fromServer);
                    while((fromServer = ftpIn.readLine()) != null) {
                        // multi-line response ends with the first response code provided
                        if (fromServer.split(" ")[0].equals(responseCode)) break;
                        // otherwise print each line
                        System.out.println("<-- " + fromServer);
                    }
                    System.out.println("<-- " + fromServer);
                } else {
                    System.out.println("<-- " + fromServer);
                }
            }
        } catch (IOException e) {
            System.out.println("0xFFFD Control connection I/O error, closing control connection.");
            System.exit(0);
        }
    }

    /**
     * Sends a USER command to the ftp server userName and calls handleServerResponse to handle the response.
     * @param args  user input
     */
    private static void handleUser(String[] args){
        if (args.length == 2) {
            String command = args[0];
            String user = args[1];

            System.out.println("--> USER " + user);
            clientOut.print("USER "+ user + "\r\n");
            clientOut.flush();

            handleServerResponse();

        } else {
            // incorrect number of arguments
            System.out.println("0x002 Incorrect number of arguments.");
        }
    }

    /**
     * Handles the pw command: sends a PASS command to the ftp server and and calls handleServerResponse to
     * handle the response.
     * @param args user input
     */
    private static void handlePass(String[] args){
        if (args.length == 2) {
            String pw = args[0];
            String password = args[1];

            System.out.println("--> PASS " + password);
            clientOut.print("PASS "+ password + "\r\n");
            clientOut.flush();

            handleServerResponse();

        } else {
            // incorrect number of arguments
            System.out.println("0x002 Incorrect number of arguments.");
        }
    }

    /**
     * Handles the quit command: sends a QUIT command to the ftp server and exits if connected
     * otherwise exits the program.
     * It calls the handleServerResponse to handle the server response if the server is connected.
     * This command is valid at anytime.
     * @param args user input
     */
    private static void handleQuit(String[] args){
        if (args.length == 1) { // check if the command is provided correctly
            if (!socket.isClosed() || socket.isConnected()) {
                System.out.println("--> QUIT");
                clientOut.print("QUIT\r\n");
                clientOut.flush();

                handleServerResponse();
            }
            // close the resources then exit.
            try {
                socket.close();
                ftpIn.close();
                clientIn.close();
                clientOut.close();
                System.exit(0);
            } catch (IOException exception) {
                System.out.println("0xFFFD Control connection I/O error, closing control connection.");
                System.exit(0);
            }
        } else {
            // incorrect number of arguments
            System.out.println("0x002 Incorrect number of arguments.");
        }
    }

    /**
     * Handles the cd command: sends a CWD command to the ftp server with the requested directory and calls
     * handleServerResponse to handle the server response.
     * @param args
     */
    private static void handleCd(String[] args){
        if (args.length == 2) {
            String cd = args[0];
            String dir = args[1];

            System.out.println("--> CWD " + dir);
            clientOut.print("CWD "+ dir + "\r\n");
            clientOut.flush();

            handleServerResponse();

        } else {
            // incorrect number of arguments
            System.out.println("0x002 Incorrect number of arguments.");
        }
    }

    /**
     * Handles the features command: sends a FEAT command to the ftp server and calls handleServerResponse to
     * handle the server response.
     * @param args
     */
    private static void handleFeatures(String[] args){
        if (args.length == 1) {

            System.out.println("--> FEAT");
            clientOut.print("FEAT" + "\r\n");
            clientOut.flush();

            handleServerResponse();

        } else {
            // incorrect number of arguments
            System.out.println("0x002 Incorrect number of arguments.");
        }
    }

    /**
     * Handles the dir command: establishes a data connection and retrieves a list of files in the current
     * working directory by sending PASV and LIST commands to the server
     * @param args
     */
    private static void handleDir(String[] args){
        if (args.length == 1) {
            String fromServer;
            String ip;
            int hostNum;
            Socket dataSocket = new Socket();

            System.out.println("--> PASV");
            clientOut.print("PASV" + "\r\n");
            clientOut.flush();

            try {
                if ((fromServer = ftpIn.readLine()) != null) {
                    System.out.println("<-- " + fromServer);
                    if (fromServer.split(" ")[0].equals("227")) { // entering passive mode; parse the ip and host
                        String ipAndHost = fromServer.split("\\(")[1];
                        String[] arguments = ipAndHost.split(",");
                        // the last number will have additional ")" so split that as well
                        arguments[5] = arguments[5].split("\\)")[0];
                        // get the ip and host to make the connection
                        ip = arguments[0] + "." + arguments[1] + "." + arguments[2] + "." + arguments[3];
                        hostNum = (Integer.parseInt(arguments[4]) * 256) + Integer.parseInt(arguments[5]);


                        try{
                            dataSocket.connect(new InetSocketAddress(ip, hostNum), 10000);
                            BufferedReader dataSocketIn = new BufferedReader(new InputStreamReader(dataSocket.getInputStream()));
                            String fromDataSocket;

                            // send the list command
                            System.out.println("--> LIST");
                            clientOut.print("LIST" + "\r\n");
                            clientOut.flush();
                            handleServerResponse();

                            // print the response
                            while((fromDataSocket = dataSocketIn.readLine()) != null) {
                                System.out.println(fromDataSocket);
                            }
                            handleServerResponse();

                            // close the connection
                            dataSocket.close();
                            dataSocketIn.close();
                        }
                        catch (IOException e) {
                            System.out.println("0x3A7 Data transfer connection I/O error, closing data connection.");
                            dataSocket.close();
                        }
                        catch (Exception exception){
                            System.out.println("0x3A2 Data transfer connection to " +
                                    ip + " on port " + hostNum + " failed to open.");
                            dataSocket.close();
                        }
                    } else {
                        System.out.println("<-- " + fromServer);
                        return;
                    }
                }
            } catch (IOException e) {
                System.out.println("0xFFFD Control connection I/O error, closing control connection.");
                System.exit(0);
            }
        } else { // incorrect number of arguments
            System.out.println("0x002 Incorrect number of arguments.");
        }
    }

    /**
     * Handles the get command: Establishes a data connection and retrieves the file indicated by second argument,
     * saving the file in a file of the same name on the local machine.
     * It sends PASV and RETR commands to the actual ftp server.
     * @param args command line arguments, 2nd argument corresponds to the file name
     */
    private static void handleGet(String[] args){
        if (args.length == 2) {
            String fileName = args[1]; // file name
            String fromServer;
            String ip;
            int hostNum;
            Socket dataSocket = new Socket(); // socket for the data connection
            int fileSize = 0;
            // According to the spec, the files transferred by the RETR command are to be in binary.
            // Request the file in binary by sending a TYPE request; server accepts it with 200



            System.out.println("--> TYPE I");
            clientOut.print("TYPE I\r\n");
            clientOut.flush();

            try {
                if ((fromServer = ftpIn.readLine()) != null) {
                    System.out.println(fromServer);
                    if (fromServer.startsWith("200")) { // now it is in binary continue
                        // in binary mode
                        // get the size to allocate byte array for the file

                        // I AM NOT PRINTING THE RESPONSE FOR THE SIZE COMMAND BECAUSE ACCORDING TO THE SPEC
                        // IT IS NOT ONE OF THE REQUIRED COMMANDS(PASV, RETR) FOR THIS COMMAND
                        clientOut.print("SIZE " + fileName + "\r\n");
                        clientOut.flush();

                        fromServer = ftpIn.readLine();

                        // get the file size
                        if (fromServer.startsWith("213")) {
                            String size = fromServer.split(" ")[1];
                            fileSize = Integer.parseInt(size);
                        } else {
                            System.out.println("<-- " + fromServer);
                            return;
                        }

                        // Request PASV
                        System.out.println("--> PASV");
                        clientOut.print("PASV" + "\r\n");
                        clientOut.flush();

                        if ((fromServer = ftpIn.readLine()) != null) {
                            System.out.println("<-- " + fromServer);
                            if (fromServer.split(" ")[0].equals("227")) { // entering passive mode; parse the ip and host
                                String ipAndHost = fromServer.split("\\(")[1];
                                String[] arguments = ipAndHost.split(",");
                                // the last number will have additional ")" so split that as well
                                arguments[5] = arguments[5].split("\\)")[0];
                                // get the ip and host to make the connection
                                ip = arguments[0] + "." + arguments[1] + "." + arguments[2] + "." + arguments[3];
                                hostNum = (Integer.parseInt(arguments[4]) * 256) + Integer.parseInt(arguments[5]);

                                // TODO ANOTHER METHOD FOR THIS MAYBE? -- THIS IS TOO LONG
                                try {
                                    dataSocket.connect(new InetSocketAddress(ip, hostNum), 10000);
                                    BufferedInputStream dataSocketIn = new BufferedInputStream(dataSocket.getInputStream());

                                    // send the list command
                                    System.out.println("--> RETR " + fileName);
                                    clientOut.print("RETR " + fileName + "\r\n");
                                    clientOut.flush();

                                    if ((fromServer = ftpIn.readLine()) != null) {
                                        if (fromServer.startsWith("125")) { // awesomeee:))))
                                            System.out.println("<-- " + fromServer);

                                            // create resources to read file in bytes
                                            byte[] bytes = new byte[fileSize];
                                            int bytesRead = 0;      // how many bytes in each read
                                            int offset = 0;
                                            
                                            
                                                // get the bytes
                                                while ((bytesRead = dataSocketIn.read(bytes, offset, bytes.length-offset)) != -1) {
                                                    offset += bytesRead;
                                                    if (offset == bytes.length) break;
                                                }
                                                try{
                                                    // System.out.println("heyyyy");
                                                    File file = new File(fileName);
                                                    FileOutputStream fileOut = new FileOutputStream(file);
                                                    fileOut.write(bytes);
                                                    fileOut.close();
                                                }
                                                catch(Exception er) {
                                                    System.out.println("0x38E Access to local file XXX denied.");
                                                    return;
                                                }
                                        }
                                        else { // request is refused
                                            System.out.println("<-- " + fromServer);
                                            dataSocket.close();
                                            dataSocketIn.close();
                                            return;
                                        }
                                    }
                                    // handle the response
                                    handleServerResponse();

                                    // close the connection
                                    dataSocket.close();
                                    dataSocketIn.close();
                                } catch (IOException e) {
                                    System.out.println("0x3A7 Data transfer connection I/O error, closing data connection.");
                                    dataSocket.close();
                                } catch (Exception exception) {
                                    System.out.println("0x3A2 Data transfer connection to " +
                                            ip + " on port " + hostNum + " failed to open. " + exception.getMessage());
                                    dataSocket.close();
                                }
                            } else {
                                System.out.println("<-- " + fromServer);
                                return;
                            }
                        }
                    } else {
                        System.out.println("<-- " + fromServer);
                        return;
                    }
                }
            } catch (IOException e) {
                System.out.println("0xFFFD Control connection I/O error, closing control connection.");
            }
        }
        else { // incorrect number of arguments
            System.out.println("0x002 Incorrect number of arguments.");
        }
    }

    
    /**
     * Gets the user command from the command line and calls appropriate command handler
     *
     */
    private static void handleCommand(){
        String command;
        String fromUser; // user input from the command line
        int argc;        // user input argument count
        String cmds[]; // command as a string array

        try {
            fromUser = clientIn.readLine();
            cmds = fromUser.split(" ");
            command = cmds[0];
            argc = cmds.length;

            switch (command) {
                // handle login
                case "user":
                    handleUser(cmds);
                    break;
                case "pw":
                    handlePass(cmds);
                    break;
                case "quit":
                    handleQuit(cmds);
                    break;
                case "get":
                    handleGet(cmds);
                    break;
                case "features":
                    handleFeatures(cmds);
                    break;
                case "cd":
                    handleCd(cmds);
                    break;
                case "dir":
                    handleDir(cmds);
                    break;
                case "":
                    break;
                case " ":
                    break;
                case "#":
                    break;
                default:
                    System.out.println("0x001 Invalid commad.");
                    break;
            }
        } catch (IOException e){
            System.out.println("0xFFFE Input error while reading commands, terminating.");
            System.exit(0);
        }

    }

    
    public static void main(String [] args)
    {
        // Get command line arguments and connected to FTP
        // If the arguments are invalid or there aren't enough of them
        // then exit.
        
        if (args.length > ARG_CNT || args.length < ARG_NO_PORT) {
            System.out.print("Usage: cmd ServerAddress ServerPort\n");
            return;
        }


        // Get the server address and port; default port is 21 if no port is provided
        byte cmdString[] = new byte[MAX_LEN];
        String serverAddress = args[0];
        int serverPort;
        if (args.length == 1){
            serverPort = 21;
        }
        else {
            try {
                serverPort = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.out.print("Usage: cmd ServerAddress ServerPort\n");
                return;
            }
        }

        // loop until quit or fatal error
        try {
            for (int len = 1; len > 0;) {

                // make sure the socket is connected
                if (socket.isClosed() || !socket.isConnected()){
                    try{
                        socket = new Socket();
                        socket.connect(new InetSocketAddress(serverAddress, serverPort), 20000);
                        ftpIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        clientOut = new PrintWriter(socket.getOutputStream());
                        clientIn = new BufferedReader(new InputStreamReader(System.in));
                    } catch (IOException e) {
                        System.out.println("0xFFFD Control connection I/O error, closing control connection.");
                        System.exit(0);
                    } catch (Exception e) {
                        System.out.println("0xFFFC Connection Error on server: " + serverAddress + " port: " + serverPort);
                        System.exit(0);
                    }
                    // if all is good: handle the first response from the server
                    handleFirst();
                }

                System.out.print("csftp> ");
                // Start processing the command here.
                handleCommand();
            }
        } catch (Exception excp){
            System.err.println("0xFFFF Processing error. " + excp.getMessage() + ".");
            System.exit(0);
        }
        }
}
