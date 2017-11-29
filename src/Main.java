import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Main {

    private static Socket socket;
    private static OutputStream outputStream;
    private static InputStream inputStream;

    public static void main(String[] args) {
        final String SERVER_ADDRESS = "localhost";
        final int SERVER_PORT = 1337;

        try {
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            inputStream = socket.getInputStream();
            outputStream = socket.getOutputStream();

            new Thread(new InputHandler()).start();
            new Thread(new OutputHandler()).start();

        } catch (IOException e) {
            System.out.println("Probleem bij opstarten");
        }
    }

    public static class OutputHandler implements Runnable {

        @Override
        public void run() {
            Scanner scanner = new Scanner(System.in);
            PrintWriter writer = new PrintWriter(outputStream);

            while(true) {
                String command = scanner.nextLine();
                writer.println(command);
                writer.flush();
            }
        }
    }

    public static class InputHandler implements Runnable {

        @Override
        public void run() {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                while(true) {
                    String line = reader.readLine();
                    System.out.println(line);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
