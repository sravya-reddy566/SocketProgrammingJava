import java.io.*;
import java.net.*;
import java.util.Scanner;

public class DateClient {
    public static void main(String[] args) {

        String serverIP = "172.16.42.102"; // replace with your server IP
        int port = 6013;

        try {
            Socket socket = new Socket(serverIP, port);
            System.out.println("Connected to server at " + serverIP + ":" + port);

            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            Scanner scanner = new Scanner(System.in);

            // Thread for receiving messages from server
            Thread receiveThread = new Thread(() -> {
                try {
                    String serverMsg;
                    while ((serverMsg = in.readLine()) != null) {
                        System.out.println("Server: " + serverMsg);
                    }
                } catch (IOException e) {
                    System.out.println("Server disconnected.");
                }
            });
            receiveThread.start();

            // Main thread: send messages to server
            while (true) {
                System.out.print("You: ");
                String msg = scanner.nextLine();
                out.println(msg);

                if (msg.equalsIgnoreCase("exit")) {
                    break;
                }
            }

            // Clean up
            socket.close();
            scanner.close();
            System.out.println("Disconnected from server.");

        } catch (IOException e) {
            System.err.println("Connection error: " + e.getMessage());
        }
    }
}