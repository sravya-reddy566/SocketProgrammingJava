import java.net.*;
import java.io.*;

public class DateServer {
    public static void main(String[] args) {
        try {
            ServerSocket server = new ServerSocket(6013);
            System.out.println("Server started on port 6013...");

            while (true) {
                Socket client = server.accept();
                System.out.println("Client connected!");

                PrintWriter out =
                        new PrintWriter(client.getOutputStream(), true);

                // ONE-WAY: Server sends data automatically
                out.println("Current Date & Time: " + new java.util.Date());

                client.close();
                System.out.println("Client disconnected!");
            }
        } catch (IOException e) {
            System.err.println(e);
        }
    }
}