import java.io.*;
import java.net.*;
import java.util.Scanner;

public class DateClient_sgarika1 {
    public static void main(String[] args) {

        // Use try-with-resources to auto-close everything
        try (
            Socket sock = new Socket("172.16.42.102", 6013);
            PrintWriter out = new PrintWriter(sock.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
            Scanner scanner = new Scanner(System.in)
        ) {
            System.out.println("Connected to server.");
            System.out.println("Type messages to send to the server. Type 'exit' to quit.");

            String response; // declare once
            while (true) {
                System.out.print("You: ");
                String message = scanner.nextLine();

                if (message.equalsIgnoreCase("exit")) {
                    break; // exit the loop
                }

                out.println(message); // send message

                // Optional: read server response
                response = in.readLine();
                if (response != null) {
                    System.out.println("Server: " + response);
                }
            }

            System.out.println("Disconnected from server.");

        } catch (IOException e) {
            System.err.println("Connection error: " + e);
        } // try-with-resources automatically closes streams and socket
    }
}
