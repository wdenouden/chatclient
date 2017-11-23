import java.io.*;
import java.net.Socket;

public class Main {

    static String SERVER_ADDRESS = "localhost";
    static int SERVER_PORT = 1337;

    static Socket socket;

    public static void main(String[] args) {
        try {
            // Maak verbinding met de server
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);

            // Haal de input- en output stream uit de socket op
            InputStream inputStream = socket.getInputStream();
            OutputStream outputStream = socket.getOutputStream();

            // Verstuur een regel tekst
            PrintWriter writer = new PrintWriter(outputStream);
            writer.println("hallo");
            writer.flush(); // Vertel het besturingssysteem om alle uitstaande data te versturen

            while(true) {
                // Blokkeer de thread tot er een volledige regel binnenkomt
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                String line = reader.readLine();
                System.out.println(line);
            }
        } catch (IOException e) {

        }
    }
}
