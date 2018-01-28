package nl.saxion.internettech;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.*;
import java.util.concurrent.Semaphore;

import static nl.saxion.internettech.ServerState.*;

public class Server {

    private ServerSocket serverSocket;
    private Set<ClientThread> threads;
    private ServerConfiguration conf;
    private GroupManager groupManager;

    public Server(ServerConfiguration conf) {
        this.conf = conf;
    }

    /**
     * Runs the server. The server listens for incoming client connections
     * by opening a socket on a specific port.
     */
    public void run() {
        // Create a socket to wait for clients.
        try {

            SSLContext context = SSLContext.getInstance("TLS");
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
            KeyStore keyStore = KeyStore.getInstance("JKS");

            keyStore.load(new FileInputStream("keystore.chatclient"), "Saxion123".toCharArray());
            keyManagerFactory.init(keyStore, "Saxion123".toCharArray());
            context.init(keyManagerFactory.getKeyManagers(), null, null);

            SSLServerSocketFactory factory = context.getServerSocketFactory();

            serverSocket = factory.createServerSocket(conf.SERVER_PORT);
            threads = new HashSet<>();
            groupManager = new GroupManager();

            while (true) {
                // Wait for an incoming client-connection request (blocking).
                Socket socket = serverSocket.accept();

                // When a new connection has been established, start a new thread.
                ClientThread ct = new ClientThread(socket);
                threads.add(ct);
                new Thread(ct).start();
                System.out.println("Num clients: " + threads.size());

                // Simulate lost connections if configured.
                if(conf.doSimulateConnectionLost()){
                    DropClientThread dct = new DropClientThread(ct);
                    new Thread(dct).start();
                }
            }
        } catch (GeneralSecurityException | IOException e ) {
            e.printStackTrace();
        }
    }

    /**
     * This thread sleeps for somewhere between 10 tot 20 seconds and then drops the
     * client thread. This is done to simulate a lost in connection.
     */
    protected class DropClientThread implements Runnable {
        ClientThread ct;

        DropClientThread(ClientThread ct){
            this.ct = ct;
        }

        public void run() {
            try {
                // Drop a client thread between 10 to 20 seconds.
                int sleep = (10 + new Random().nextInt(10)) * 1000;
                Thread.sleep(sleep);
                ct.kill();
                threads.remove(ct);
                System.out.println("Num clients: " + threads.size());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * This inner class is used to handle all communication between the server and a
     * specific client.
     */
    protected class ClientThread implements Runnable {

        private DataInputStream is;
        private OutputStream os;
        private Socket socket;
        private ServerState state;
        private String username;

        public ClientThread(Socket socket) {
            this.state = INIT;
            this.socket = socket;
        }

        public String getUsername() {
            return username;
        }

        public OutputStream getOutputStream() {
            return os;
        }

        public void run() {
            try {
                // Create input and output streams for the socket.
                os = socket.getOutputStream();
                is = new DataInputStream(socket.getInputStream());
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));

                // According to the protocol we should send HELO <welcome message>
                state = CONNECTING;
                String welcomeMessage = "HELO " + conf.WELCOME_MESSAGE;
                writeToClient(welcomeMessage);

                while (!state.equals(FINISHED)) {
                    // Wait for message from the client.
                    String line = reader.readLine();
                    if (line != null) {
                        // Log incoming message for debug purposes.
                        boolean isIncomingMessage = true;
                        logMessage(isIncomingMessage, line);

                        // Parse incoming message.
                        Message message = new Message(line);

                        if(username != null) {
                            // Process message.
                            switch (message.getMessageType()) {
                                case HELO:
                                    // Already logged in
                                    writeToClient("-ERR Already logged in as " + username);
                                    break;
                                case BCST:
                                    // Broadcast to other clients.
                                    broadcast(message);
                                    break;
                                case QUIT:
                                    // Close connection
                                    state = FINISHED;
                                    writeToClient("+OK Goodbye");
                                    break;
                                case UNKOWN:
                                    // Unkown command has been sent
                                    writeToClient("-ERR Unkown command");
                                    break;
                                case USERS:
                                    // Show all online users
                                    showUsers();
                                    break;
                                case DM :
                                    // Send private message to user
                                    sendPrivateMessage(line);
                                    break;
                                case ADD:
                                    // Add new group
                                    addGroup(line);
                                    break;
                                case JOIN:
                                    // Join existing group
                                    joinGroup(line);
                                    break;
                                case GROUPS:
                                    // Show all groups with users
                                    showGroups();
                                    break;
                                case LEAVE:
                                    // Leave group
                                    leaveGroup(line);
                                    break;
                                case GROUPBCST:
                                    // Send message to all users in group
                                    groupBroadcast(line);
                                    break;
                                case KICK:
                                    // Kick user from group
                                    kickUser(line);
                                    break;
                                case SENDFILE:
                                    // Send file to user
                                    sendFile(line);
                                    break;
                                case COMMANDS:
                                    // Show all commands
                                    showCommands();
                                    break;
                            }
                        } else if(message.getMessageType() == Message.MessageType.HELO) {
                            // Check username format.
                            createUser(message);
                        }
                    }
                }
                // Remove from the list of client threads and close the socket.
                threads.remove(this);
                socket.close();
            } catch (IOException e) {
                System.out.println("Server Exception: " + e.getMessage());
            }
        }

        private void createUser(Message message) {
            boolean isValidUsername = message.getPayload().matches("[a-zA-Z0-9_]{3,14}");
            if(!isValidUsername) {
                state = FINISHED;
                writeToClient("-ERR username has an invalid format (only characters, numbers and underscores are allowed)");
            } else {
                // Check if user already exists.
                boolean userExists = false;
                for (ClientThread ct : threads) {
                    if (ct != this && message.getPayload().equals(ct.getUsername())) {
                        userExists = true;
                        break;
                    }
                }
                if (userExists) {
                    writeToClient("-ERR user already logged in");
                } else {
                    state = CONNECTED;
                    this.username = message.getPayload();
                    writeToClient("+OK " + getUsername());
                }
            }
        }

        /**
         * Broadcast message to other users
         * @param message
         */
        private void broadcast(Message message) {
            for (ClientThread ct : threads) {
                if (ct != this) {
                    ct.writeToClient("BCST [" + getUsername() + "] " + message.getPayload());
                }
            }
            writeToClient("+OK");
        }

        /**
         * Show all users
         */
        private void showUsers() {
            for(ClientThread ct: threads) {
                if(ct.getUsername() != null) {
                    writeToClient(ct.getUsername());
                }
            }
        }

        /**
         * Send message to one user
         * @param line
         */
        private void sendPrivateMessage(String line) {
            if(line != null && line.length() > 0) {
                String[] splits = line.split("\\s+");
                if(splits.length > 2) {
                    for(ClientThread ct: threads) {
                        if(ct.getUsername().equals(splits[1])) {
                            String msg = "DM " + getUsername() + " says: ";
                            for(int i = 2; i < splits.length; i++) {
                                msg += splits[i] + " ";
                            }
                            ct.writeToClient(msg);
                            writeToClient("+OK message sent");
                        }
                    }
                }
            }
        }

        /**
         * Add new group
         * @param line
         */
        private void addGroup(String line) {
            if(line != null && line.length() > 0) {
                String[] splits = line.split("\\s+");
                if (splits.length > 1) {
                    String groupName = splits[1];
                    Group group = new Group(groupName, this);
                    writeToClient(groupManager.addGroup(group));
                }
            }
        }

        /**
         * Join existing group
         * @param line
         */
        private void joinGroup(String line) {
            if(line != null && line.length() > 0) {
                String[] splits = line.split("\\s+");
                if (splits.length > 1) {
                    String groupName = splits[1];
                    writeToClient(groupManager.joinGroup(groupName, this));
                }
            }
        }

        /**
         * Show all groups with their members
         */
        private void showGroups() {
            writeToClient(groupManager.showGroups());
        }

        /**
         * Leave group
         * @param line
         */
        private void leaveGroup(String line) {
            if(line != null && line.length() > 0) {
                String[] splits = line.split("\\s+");
                if (splits.length > 1) {
                    String groupName = splits[1];
                    writeToClient(groupManager.leaveGroup(groupName, this));
                }
            }
        }

        /**
         * Send message to all users in group
         * @param line
         */
        private void groupBroadcast(String line) {
            if(line != null && line.length() > 0) {
                String[] splits = line.split("\\s+");
                if(splits.length > 2) {
                    writeToClient(groupManager.sendGroupMessage(splits, getUsername()));
                }
            }
        }

        /**
         * Kick user from group
         * @param line
         */
        private void kickUser(String line) {
            if(line != null && line.length() > 0) {
                String[] splits = line.split("\\s+");
                if (splits.length > 2) {
                    String groupname = splits[1];
                    String username = splits[2];
                    writeToClient(groupManager.kickUser(groupname, username, getUsername()));
                }
            }
        }

        /**
         * Send file to other user
         * @param line
         */
        private void sendFile(String line) {
            if(line != null && line.length() > 0) {
                String[] splits = line.split("\\s+");
                if(splits.length > 3) {
                    String username = splits[1];
                    String filename = splits[2];
                    String base64 = splits[3];

                    ClientThread sendToUser = null;
                    for (ClientThread ct : threads) {
                        if (ct != this && username.equals(ct.getUsername())) {
                            sendToUser = ct;
                            break;
                        }
                    }
                    if (sendToUser == null) {
                        writeToClient("-ERR user not found");
                    } else {
                        writeToClient("+OK");
                        sendToUser.writeToClient("FILE " + getUsername() + " " + filename + " " + base64);
                    }
                }
            }
        }

        /**
         * Show all available commands
         */
        private void showCommands() {
            String msg = "+OK COMMANDS ARE:";
            for(Message.MessageType type: Message.MessageType.values()) {
                msg +=  " " + type;
            }
            writeToClient(msg);
        }

        /**
         * An external process can stop the client using this methode.
         */
        public void kill() {
            try {
                // Log connection drop and close the outputstream.
                System.out.println("[DROP CONNECTION] " + getUsername());
                threads.remove(this);
                socket.close();
            } catch(Exception ex) {
                System.out.println("Exception when closing outputstream: " + ex.getMessage());
            }
            state = FINISHED;
        }

        /**
         * Write a message to this client thread.
         * @param message   The message to be sent to the (connected) client.
         */
        protected void writeToClient(String message) {
            boolean shouldDropPacket = false;
            boolean shouldCorruptPacket = false;

            // Check if we need to behave badly by dropping some messages.
            if (conf.doSimulateDroppedPackets()) {
                // Randomly select if we are going to drop this message or not.
                int random = new Random().nextInt(6);
                if (random == 0) {
                    // Drop message.
                    shouldDropPacket = true;
                    System.out.println("[DROPPED] " + message);
                }
            }

            // Check if we need to behave badly by corrupting some messages.
            if (conf.doSimulateCorruptedPackets()) {
                // Randomly select if we are going to corrupt this message or not.
                int random = new Random().nextInt(4);
                if (random == 0) {
                    // Corrupt message.
                    shouldCorruptPacket = true;
                }
            }

            // Do the actual message sending here.
            if (!shouldDropPacket) {
                if (shouldCorruptPacket){
                    message = corrupt(message);
                    System.out.println("[CORRUPT] " + message);
                }
                PrintWriter writer = new PrintWriter(os);
                writer.println(message);
                writer.flush();

                // Echo the message to the server console for debugging purposes.
                boolean isIncomingMessage = false;
                logMessage(isIncomingMessage, message);
            }
        }

        /**
         * This methods implements a (naive) simulation of a corrupt message by replacing
         * some charaters at random indexes with the charater X.
         * @param message   The message to be corrupted.
         * @return  Returns the message with some charaters replaced with X's.
         */
        private String corrupt(String message) {
            Random random = new Random();
            int x = random.nextInt(4);
            char[] messageChars =  message.toCharArray();

            while (x < messageChars.length) {
                messageChars[x] = 'X';
                x = x + random.nextInt(10);
            }

            return new String(messageChars);
        }

        /**
         * Util method to print (debug) information about the server's incoming and outgoing messages.
         * @param isIncoming    Indicates whether the message was an incoming message. If false then
         *                      an outgoing message is assumed.
         * @param message       The message received or sent.
         */
        private void logMessage(boolean isIncoming, String message) {
            String logMessage;
            String colorCode = conf.CLI_COLOR_OUTGOING;
            String directionString = ">> ";  // Outgoing message.
            if (isIncoming) {
                colorCode = conf.CLI_COLOR_INCOMING;
                directionString = "<< ";     // Incoming message.
            }

            // Add username to log if present.
            // Note when setting up the connection the user is not known.
            if (getUsername() == null) {
                logMessage = directionString + message;
            } else {
                logMessage = directionString + "[" + getUsername() + "] " + message;
            }

            // Log debug messages with or without colors.
            if(conf.isShowColors()){
                System.out.println(colorCode + logMessage + conf.RESET_CLI_COLORS);
            } else {
                System.out.println(logMessage);
            }
        }
    }
}
