import java.io.*;
import java.net.*;
import java.util.Scanner;

public class DateClient {

    public static void main(String[] args) {
        String serverIp = "172.16.58.53"; // change to server machine IP
        int port = 6013;

        try (
            Socket socket = new Socket(serverIp, port);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            Scanner scanner = new Scanner(System.in)
        ) {
            // Reader thread (so you see "Enter your name:" immediately)
            Thread reader = new Thread(() -> {
                try {
                    String line;
                    while ((line = in.readLine()) != null) {
                        System.out.println(line);
                    }
                } catch (IOException ignored) {}
            });
            reader.setDaemon(true);
            reader.start();

            // Writer loop
            while (true) {
                String msg = scanner.nextLine();
                out.println(msg);

                if (msg.equalsIgnoreCase("exit") || msg.equalsIgnoreCase("bye")) {
                    break;
                }
            }

        } catch (IOException e) {
            System.out.println("Connection error: " + e.getMessage());
        }
    }
}
