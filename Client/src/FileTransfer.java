import java.io.*;
import java.util.Base64;

public class FileTransfer {

    public boolean checkIfFileExists(String filename) {
        File file = new File(filename);
        return file.exists();
    }

    public String fileToBase64(String filename) {
        try {
            if (checkIfFileExists(filename)) {
                File file = new File(filename);
                InputStream fileInput = new FileInputStream(file);
                byte[] fileBytes = new byte[(int) file.length()];
                fileInput.read(fileBytes, 0, fileBytes.length);
                fileInput.close();
                return Base64.getEncoder().encodeToString(fileBytes);
            }
        } catch (IOException e) {
            return null;
        }

        return null;
    }

    public boolean saveFileFromBase64(String filename, String base64) {
        try {
            File f = new File(filename);
            int fileNumber = 1;
            while (f.exists()) {
                String[] fileSplit = filename.split("\\.(?=[^\\.]+$)");
                f = new File(fileSplit[0] + "(" + fileNumber + ")" + fileSplit[1]);
                fileNumber++;
            }
            byte[] data = Base64.getDecoder().decode(base64);
            OutputStream stream = new FileOutputStream(f);
            stream.write(data);
            stream.close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
