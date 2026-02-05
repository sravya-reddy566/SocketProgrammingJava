import java.net.*;
import java.io.*;

public class DateClient {
    public static void main(String[] args) {
        try {
            Socket sock = new Socket("127.0.0.1", 6013);

            BufferedReader bin =
                    new BufferedReader(
                            new InputStreamReader(sock.getInputStream())
                    );

            String response;
            while ((response = bin.readLine()) != null) {
                System.out.println(response);
            }

            sock.close();
        } catch (IOException e) {
            System.err.println(e);
        }
    }
}