import java.net.*;
import java.io.*;
import java.util.Scanner;

public class DateServer {
    public static void main(String[] args) throws Exception {

        ServerSocket server = new ServerSocket(6013);
        System.out.println("Server started. Waiting for client...");

        Socket client = server.accept();
        System.out.println("Client connected.");

        BufferedReader in =
                new BufferedReader(new InputStreamReader(client.getInputStream()));
        PrintWriter out =
                new PrintWriter(client.getOutputStream(), true);

        
        new Thread(() -> {
            try {
                String msg;
                while ((msg = in.readLine()) != null) {
                    System.out.println("Client: " + msg);
                }
            } catch (Exception e) {
                System.out.println("Client disconnected.");
            }
        }).start();

      
        Scanner sc = new Scanner(System.in);
        while (true) {
            String msg = sc.nextLine();
            out.println(msg);
            if (msg.equalsIgnoreCase("bye")) break;
        }

        client.close();
        server.close();
        System.out.println("Chat ended.");
    }
}