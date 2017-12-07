import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class InputHandler extends Thread {

    private Socket socket;
    private IMessageReceivedHandler onReceived;

    public InputHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }

        while(true) {
            String line = null;
            try {
                line = reader.readLine();
                onReceived.onReceived(line);
            } catch (IOException e) {
                System.out.println("Could not read message");
                return;
            }
        }
    }

    public void setOnMessageReceived(IMessageReceivedHandler handler) {
        this.onReceived = handler;
    }

    public interface IMessageReceivedHandler {
        public void onReceived(String message);
    }
}
