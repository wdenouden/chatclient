import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class OutputHandler extends Thread {

    private Socket socket;
    private IMessageSentHandler onSentHandler;

    public OutputHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {

        Scanner scanner = new Scanner(System.in);
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(socket.getOutputStream());
        } catch (IOException e) {
            System.out.println("Could not get outputstream");
            return;
        }

        while(true) {
            String command = scanner.nextLine();
            writer.println(command);
            writer.flush();
        }
    }

    public void setOnMessageSent(IMessageSentHandler handler) {
        this.onSentHandler = handler;
    }

    public interface IMessageSentHandler {
        public void onSent(String message);
    }
}
