package nl.saxion.internettech;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

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
            serverSocket = new ServerSocket(conf.SERVER_PORT);
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class Group {

        private String groupName;
        private String ownerName;
        private ArrayList<ClientThread> users;

        public Group(String groupName, ClientThread owner) {
            this.groupName = groupName;
            users = new ArrayList<ClientThread>();
            ownerName = owner.getUsername();
            users.add(owner);
        }

        public String getGroupName() {
            return groupName;
        }

        public String getOwnerName() {
            return ownerName;
        }

        public ArrayList<ClientThread> getUsers() {
            return users;
        }

        public boolean joinGroup(ClientThread ct) {
            if(!userExists(ct.getUsername())) {
                users.add(ct);
                return true;
            } else {
                return false;
            }
        }

        public String leaveGroup(ClientThread ct) {
            // Als owner de groep verlaat, wordt de eerst volgende persoon de owner
            // als er geen andere gebruikers zijn, wordt de groep verwijderd
            if(ct.getUsername().equals(ownerName)) {
                if(users.size() > 1) {
                    ownerName = users.get(1).getUsername();
                } else {
                    return "DEL";
                }
            }

            for(ClientThread user: users) {
                if(ct.getUsername().equals((user.getUsername()))) {
                    users.remove(ct);
                    return "OK";
                }
            }
            return "ERR";
        }

        public void sendMessage(String message) {
            for(ClientThread ct: users) {
                ct.writeToClient(message);
            }
        }

        public void kickUser(String username, String ownerName) {
            if(this.ownerName.equals(ownerName)) {
                for(ClientThread ct: users) {
                    if(ct.getUsername().equals(username)) {
                        users.remove(ct);
                    }
                }
            }
        }

        public String showUsers() {
            String message = "";
            for(ClientThread ct: users) {
                if(ct.getUsername() != null) {
                    message += ct.getUsername() + "\n";
                }
            }
            return message;
        }

        private boolean userExists(String username) {
            for(ClientThread user: users) {
                if(user.getUsername().equals(username)) {
                    return true;
                }
            }
            return false;
        }
    }

    private class GroupManager {

        private ArrayList<Group> groups;

        public GroupManager() {
            groups = new ArrayList<>();
        }

        public String addGroup(Group group) {
            if(!groupExists(group.getGroupName())) {
                groups.add(group);
                return "+OK group added";
            } else {
                return "-ERR group already exists";
            }
        }

        public String joinGroup(String groupName, ClientThread ct) {
            if(groupExists(groupName)) {
                for(Group g: groups) {
                    if(g.getGroupName().equals(groupName)) {
                        if(g.joinGroup(ct)) {
                            return "+OK group joined";
                        } else {
                            return "-ERR already in group";
                        }
                    }
                }
                return "-ERR group doesn't exist";
            } else {
                return "-ERR group doesn't exist";
            }
        }

        public String leaveGroup(String groupName, ClientThread ct) {
            if(groupExists(groupName)) {
                for(Group g: groups) {
                    if(g.getGroupName().equals(groupName)) {
                        switch(g.leaveGroup(ct)) {
                            case "DEL":
                                if(deleteGroup(g)) {
                                    return "+OK group deleted";
                                } else {
                                    return "-ERR ";
                                }
                            case "OK":
                                return "+OK group left";
                            case "ERR":
                                return "-ERR user not in group";
                        }
                    }
                }
                return "-ERR group doesn't exist";
            } else {
                return "-ERR group doesn't exist";
            }
        }

        public boolean deleteGroup(Group group) {
            for(Group g: groups) {
                if(g.getGroupName().equals(group.getGroupName())) {
                    groups.remove(group);
                    return true;
                }
            }
            return false;
        }

        public String sendGroupMessage(String[] splits, String username) {
            String groupName = splits[1];
            String msg = "(" + groupName + ") " + username + " says: ";
            for(int i = 2; i < splits.length; i++) {
                msg += splits[i] + " ";
            }

            for(Group g: groups) {
                if(g.getGroupName().equals(groupName)) {
                    for(ClientThread ct: g.getUsers()) {
                        if(ct.getUsername().equals(username)) {
                            g.sendMessage(msg);
                            return "+OK message sent to group";
                        }
                    }
                    return "-ERR not in group";
                }
            }
            return "-ERR group not found";
        }

        public String showGroups() {
            String msg = "";
            for(Group g: groups) {
                msg += g.getGroupName() + "\n";
            }
            return msg;
        }

        private boolean groupExists(String groupName) {
            for(Group g: groups) {
                if(g.groupName.equals(groupName)) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * This thread sleeps for somewhere between 10 tot 20 seconds and then drops the
     * client thread. This is done to simulate a lost in connection.
     */
    private class DropClientThread implements Runnable {
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
    private class ClientThread implements Runnable {

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

                        // Process message.
                        switch (message.getMessageType()) {
                            case HELO:
                                // Check username format.
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
                                break;
                            case BCST:
                                // Broadcast to other clients.
                                for (ClientThread ct : threads) {
                                    if (ct != this) {
                                        ct.writeToClient("BCST [" + getUsername() + "] " + message.getPayload());
                                    }
                                }
                                writeToClient("+OK");
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
                                for(ClientThread ct: threads) {
                                    if(ct.getUsername() != null) {
                                        writeToClient(ct.getUsername());
                                    }
                                }
                                break;
                            case DM :
                                if(line != null && line.length() > 0) {
                                    String[] splits = line.split("\\s+");
                                    if(splits.length > 2) {
                                        for(ClientThread ct: threads) {
                                            if(ct.getUsername().equals(splits[1])) {
                                                String msg = getUsername() + " says: ";
                                                for(int i = 2; i < splits.length; i++) {
                                                    msg += splits[i] + " ";
                                                }
                                                ct.writeToClient(msg);
                                                writeToClient("+OK message sent");
                                            }
                                        }
                                    }
                                }
                                break;
                            case ADD:
                                if(line != null && line.length() > 0) {
                                    String[] splits = line.split("\\s+");
                                    if (splits.length > 1) {
                                        String groupName = splits[1];
                                        Group group = new Group(groupName, this);
                                        writeToClient(groupManager.addGroup(group));
                                    }
                                }
                                break;
                            case JOIN:
                                if(line != null && line.length() > 0) {
                                    String[] splits = line.split("\\s+");
                                    if (splits.length > 1) {
                                        String groupName = splits[1];
                                        writeToClient(groupManager.joinGroup(groupName, this));
                                    }
                                }
                                break;
                            case GROUPS:
                                writeToClient(groupManager.showGroups());
                                break;
                            case LEAVE:
                                if(line != null && line.length() > 0) {
                                    String[] splits = line.split("\\s+");
                                    if (splits.length > 1) {
                                        String groupName = splits[1];
                                        writeToClient(groupManager.leaveGroup(groupName, this));
                                    }
                                }
                                break;
                            case GROUPBCST:
                                if(line != null && line.length() > 0) {
                                    String[] splits = line.split("\\s+");
                                    if(splits.length > 2) {
                                        writeToClient(groupManager.sendGroupMessage(splits, getUsername()));
                                    }
                                }
                                break;
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
        private void writeToClient(String message) {
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
