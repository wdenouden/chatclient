import model.SocketState;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ScannerThread extends Thread {

    private ScannerReadLine handler;
    private boolean running = true;

    @Override
    public void run() {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        try {
            while (running) {
                if (br.ready()) {
                    String text = br.readLine();
                    handler.lineRead(text);
                }

                sleep(20);
            }
        } catch (InterruptedException | IOException e) {
            handler.exceptionOccured();
        }
    }

    public void setOnMessageRead(ScannerReadLine handler) {
        this.handler = handler;
    }

    public void stopRunning() {
        running = false;
    }

    public interface ScannerReadLine {
        public void lineRead(String message);
        public void exceptionOccured();
    }
}



