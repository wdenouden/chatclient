import model.Message;
import model.SocketState;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.*;

public class Chatclient implements InputHandler.IMessageReceivedHandler{

    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 1337;
    private static SocketState state = SocketState.LOGIN_INPUT;

    private String username;
    private LinkedList<Message> sentMessages = new LinkedList<>();

    PrintWriter writer = null;

    private Socket socket;

    public void start() {
        while(true) {
            try {
                state = SocketState.LOGIN_INPUT;
                socket = new Socket(SERVER_ADDRESS, SERVER_PORT);

                InputHandler inputHandler = new InputHandler(socket);
                inputHandler.setOnMessageReceived(this);
                inputHandler.start();

                Scanner scanner = new Scanner(System.in);
                try {
                    writer = new PrintWriter(socket.getOutputStream());
                } catch (IOException e) {
                    System.out.println("Could not get outputstream");
                    return;
                }

                while (state != SocketState.CLOSED) {
                    switch (state) {
                        case LOGIN_INPUT:
                            while(state != SocketState.CLOSED) {
                                System.out.println("Please enter your username:");
                                String name = scanner.nextLine();

                                if(name.matches("[a-zA-Z0-9_]{3,14}")) {
                                    writer.println("HELO " + name);
                                    username = name;
                                    writer.flush();
                                    state = SocketState.LOGIN_CONFIRMING;
                                    break;
                                }else {
                                    if(state != SocketState.CLOSED) {
                                        //Press enter to reconnect, dan hoef je deze log niet te zien.
                                        System.out.println("Username has an invalid format (only characters, numbers and underscores are allowed)");
                                    }
                                }
                            }
                            break;
                        case LOGIN_CONFIRMING:
                            //Just wait
                            break;
                        case AUTHORIZED:
                            writer.println(scanner.nextLine());
                            writer.flush();
                            break;
                    }
                }
            } catch (IOException e) {
                state = SocketState.LOGIN_INPUT;
                System.out.println("Could not connect to server, attempting again in 500ms.");
            }
        }
    }

    private void sendMessage(Message m) {
        sentMessages.add(m);

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                if(sentMessages.peek() == m) {
                    System.out.println("[RESENDING] " + m.getFullMessage());
                    sentMessages.pop();
                    sendMessage(m);
                }

            }
        }, 500);

        System.out.println("[SENDING] " + m.getFullMessage());
        writer.println(m.getFullMessage());
        writer.flush();
    }

    @Override
    public void onReceived(String message) {

        Message sentMessage = null;
        if(sentMessages.size() != 0) {
            sentMessage = sentMessages.pop();
        }

        if(state == SocketState.LOGIN_CONFIRMING) {
            if(message.equals("+OK " + username)) {
                //Goed ingelogd, ga maar door.
                state = SocketState.AUTHORIZED;
                System.out.println("You are now logged in as " + username);
            }else if(message.equals("-ERR user already logged in")) {
                //Niet goed ingelogd, andere naam proberen.
                System.out.println("User already logged in, try another name.");
                username = null;
                state = SocketState.LOGIN_INPUT;
            }else {
                //Corrupted name, verstuur bericht nogmaals.
                System.out.println("Something went wrong on the server, logging in again.");
                sendMessage(sentMessage); //sentMessage moet wel het inlogbericht zijn geweest.
            }
        }else {
            System.out.println(message);
        }
    }

    @Override
    public void onConnectionLost() {
        state = SocketState.CLOSED;
    }
}
