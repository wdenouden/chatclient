import model.Message;
import model.SocketState;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.*;

public class Chatclient implements InputHandler.IMessageReceivedHandler {

    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 1337;
    private static SocketState state = SocketState.LOGIN_INPUT;

    private String username;
    private LinkedList<Message> sentMessages = new LinkedList<>();

    PrintWriter writer = null;

    private Socket socket;

    public void start() {
        while (true) {
            try {
                state = SocketState.LOGIN_INPUT;
                socket = new Socket(SERVER_ADDRESS, SERVER_PORT);

                InputHandler inputHandler = new InputHandler(socket);
                inputHandler.setOnMessageReceived(this);
                inputHandler.start();

                try {
                    writer = new PrintWriter(socket.getOutputStream());
                } catch (IOException e) {
                    System.out.println("Could not get outputstream");
                    return;
                }

                while (state != SocketState.CLOSED) {
                    switch (state) {
                        case LOGIN_INPUT:
                            if(username == null) {
                                //Nog niet ingelogd
                                System.out.println("Please enter your username.");
                                String name = readLine();

                                if (name != null) {
                                    if (name.matches("[a-zA-Z0-9_]{3,14}")) {
                                        username = name;
                                        sendMessage(new Message("HELO " + name));
                                        state = SocketState.LOGIN_CONFIRMING;
                                        break;
                                    } else {
                                        System.out.println("Username has an invalid format (only characters, numbers and underscores are allowed)");
                                    }
                                }
                            }else {
                                //Al eerder ingelogd maar gedisconnect
                                sendMessage(new Message("HELO " + username));
                                state = SocketState.LOGIN_CONFIRMING;
                            }
                            break;
                        case LOGIN_CONFIRMING:
                            //Just wait
                            break;
                        case AUTHORIZED:
                            String line = readLine();
                            if (line != null) {
                                writer.println(line);
                                writer.flush();
                            }
                            break;
                    }
                }

                socket.close();
            } catch (IOException e) {

                state = SocketState.LOGIN_INPUT;

                try {
                    System.out.println("Could not connect to server, attempting again in 500ms.");
                    Thread.sleep(500);
                } catch (InterruptedException e1) {
                    //Hoort hier echt nooit in te komen.
                    System.out.println("Unexpected error occured.");
                }
            }
        }
    }

    private void sendMessage(Message m) {
        if (!state.equals(SocketState.CLOSED)) {
            m.increaseAttempts();
            sentMessages.add(m);

            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    if (!state.equals(SocketState.CLOSED)) {
                        if (sentMessages.peek() == m) {
                            Message mResend = sentMessages.pop();
                            mResend.increaseAttempts();
                            sendMessage(mResend);
                        }
                    }
                }
            }, 500);

            writer.println(m.getMessage());
            writer.flush();
        }
    }

    @Override
    public void onReceived(String message) {

        Message sentMessage = null;
        if (sentMessages.size() != 0) {
            sentMessage = sentMessages.pop();
        }

        if (state == SocketState.LOGIN_CONFIRMING) {
            if (message.equals("+OK " + username)) {
                //Goed ingelogd, ga maar door.
                state = SocketState.AUTHORIZED;
                System.out.println("You are now logged in as " + username);
            } else if (message.equals("-ERR user already logged in")) {
                //Niet goed ingelogd, andere naam proberen.
                System.out.println("User already logged in, try another name.");
                username = null;
                state = SocketState.LOGIN_INPUT;
            } else if (message.equals("HELO Welkom to WhatsUpp!")){
                //Negeer deze, te snel ingelogt.
            }else {
                //Corrupted name, verstuur bericht nogmaals.
                System.out.println("Something went wrong on the server, logging in again.");
                sendMessage(sentMessage); //sentMessage moet wel het inlogbericht zijn geweest.
            }
        } else {
            System.out.println(message);
        }
    }

    @Override
    public void onConnectionLost() {
        sentMessages.clear();
        state = SocketState.CLOSED;
    }

    public String readLine() {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        while (!state.equals(SocketState.CLOSED)) {
            try {
                if (br.ready()) {
                    return br.readLine();
                }
            } catch (IOException e) {
                return null;
            }
        }
        return null;
    }
}
