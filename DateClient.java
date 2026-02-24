import java.io.*;
import java.net.*;
import java.util.Scanner;

public class DateClient {

    private static final Object PRINT_LOCK = new Object();

    public static void main(String[] args) {
        String serverIp = "172.16.58.53";
        int port = 6013;

        try (
            Socket sock = new Socket(serverIp, port);
            BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
            PrintWriter out = new PrintWriter(sock.getOutputStream(), true);
            Scanner scanner = new Scanner(System.in)
        ) {
            safePrintln("Connected to server " + serverIp + ":" + port);

            // Reader thread: prints anything from server immediately
            Thread reader = new Thread(() -> {
                try {
                    String line;
                    while ((line = in.readLine()) != null) {
                        synchronized (PRINT_LOCK) {
                           
                            System.out.print("\r");        
                            System.out.println(line);      
                            System.out.print("You: ");      
                            System.out.flush();
                        }
                    }
                } catch (IOException e) {
                    safePrintln("\n[Disconnected from server]");
                }
            });
            reader.setDaemon(true);
            reader.start();
            while (true) {
                synchronized (PRINT_LOCK) {
                    System.out.print("You: ");
                    System.out.flush();
                }

                String msg = scanner.nextLine();
                out.println(msg);

                if (msg.equalsIgnoreCase("exit") || msg.equalsIgnoreCase("bye")) {
                    break;
                }
            }

            safePrintln("Client closed.");

        } catch (IOException e) {
            safePrintln("Connection error: " + e.getMessage());
        }
    }

    private static void safePrintln(String s) {
        synchronized (PRINT_LOCK) {
            System.out.println(s);
        }
    }
}
