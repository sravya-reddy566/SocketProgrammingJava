import java.io.*;
import java.net.*;
import java.util.Scanner;

public class DateClient {

    public static void main(String[] args) {
        String serverIp = "172.16.58.53";   
        int port = 6013;

        try (
            Socket sock = new Socket(serverIp, port);
            BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
            PrintWriter out = new PrintWriter(sock.getOutputStream(), true);
            Scanner scanner = new Scanner(System.in)
        ) {
            System.out.println("Connected to server " + serverIp + ":" + port);

            Thread reader = new Thread(() -> {
                try {
                    String line;
                    while ((line = in.readLine()) != null) {
                        System.out.println(line);
                        System.out.print("You: ");
                    }
                } catch (IOException e) {
                    System.out.println("\n[Disconnected from server]");
                }
            });
            reader.setDaemon(true);
            reader.start();

            // Writer loop
            while (true) {
                System.out.print("You: ");
                String msg = scanner.nextLine();
                out.println(msg);

                if (msg.equalsIgnoreCase("exit") || msg.equalsIgnoreCase("bye")) {
                    break;
                }
            }

            System.out.println("Client closed.");

        } catch (IOException e) {
            System.out.println("Connection error: " + e.getMessage());
        }
    }
}
