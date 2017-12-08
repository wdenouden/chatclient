import model.Message;
import model.SocketState;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.*;

public class Chatclient implements InputHandler.IMessageReceivedHandler{

    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 1337;
    private static SocketState state = SocketState.CLOSED;

    private LinkedList<Message> sentMessages = new LinkedList<>();

    PrintWriter writer = null;

    private Socket socket;

    public void start() {
        try {
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
                        System.out.println("Please enter your username:");
                        String command = scanner.nextLine();
                        writer.println(command);
                        writer.flush();
                        state = SocketState.LOGIN_CONFIRMING;
                        break;
                    case LOGIN_CONFIRMING:
                        //Just wait
                        break;
                    case LOGIN_FAILED:
                        //Try logging in again with the name entered before

                        break;
                }
            }

//            while(state != SocketState.CLOSED) {
//                String command = scanner.nextLine();
//                writer.println(command);
//                writer.flush();
//            }
        } catch (IOException e) {
            System.out.println("Probleem bij opstarten");
        }
    }

    private void sendMessage(Message m) {
        sentMessages.add(m);

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                if(sentMessages.peek() == m) {
                    System.out.println("[RESENDING] " + m.getFullMessage());
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
        System.out.println(message);
    }
}
