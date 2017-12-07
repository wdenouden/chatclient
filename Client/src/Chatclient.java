import java.io.IOException;
import java.net.Socket;

public class Chatclient {

    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 1337;

    private Socket socket;

    public void start() {
        try {
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);

            InputHandler inputHandler = new InputHandler(socket);
            OutputHandler outputHandler = new OutputHandler(socket);

            inputHandler.setOnMessageReceived(new InputHandler.IMessageReceivedHandler() {
                @Override
                public void onReceived(String message) {
                    System.out.println(message);
                }
            });

            outputHandler.setOnMessageSent(new OutputHandler.IMessageSentHandler() {
                @Override
                public void onSent(String message) {
                    System.out.println(message);
                }
            });

            inputHandler.start();
            outputHandler.start();


        } catch (IOException e) {
            System.out.println("Probleem bij opstarten");
        }
    }
}
