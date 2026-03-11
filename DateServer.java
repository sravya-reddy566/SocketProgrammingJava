import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

public class DateServer {
    private static final int PORT = 6013;
    private static final ConcurrentHashMap<String, ClientHandler> clients = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        System.out.println("Server starting on port " + PORT + " ...");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            startServerConsole();
            System.out.println("Waiting for clients...");

            while (true) {
                Socket socket = serverSocket.accept();
                new ClientHandler(socket).start(); // multi clients simultaneously
            }
        } catch (IOException e) {
            System.out.println("Server error: " + e.getMessage());
        }
    }

    private static void startServerConsole() {
        Thread t = new Thread(() -> {
            Scanner sc = new Scanner(System.in);
            System.out.println("SERVER CONSOLE READY");
            System.out.println("Commands: list | to <name> <message>");
            System.out.println("-------------------------------------");

            while (true) {
                String line = sc.nextLine();
                if (line == null) continue;
                line = line.trim();
                if (line.isEmpty()) continue;

                if (line.equalsIgnoreCase("list")) {
                    System.out.println(getClientSummary());
                    continue;
                }

                if (line.toLowerCase().startsWith("to ")) {
                    String[] parts = line.split("\\s+", 3);
                    if (parts.length < 3) {
                        System.out.println("Usage: to <name> <message>");
                        continue;
                    }
                    String to = parts[1];
                    String msg = parts[2];

                    boolean ok = sendToClient(to, "SERVER: " + msg);
                    System.out.println(ok ? ("Sent to " + to) : ("No such client: " + to));
                    continue;
                }

                System.out.println("Unknown command. Use: list OR to <name> <message>");
            }
        });

        t.setDaemon(true);
        t.start();
    }

    private static String getClientSummary() {
        int count = clients.size();
        if (count == 0) return "Active clients (0): none";
        return "Active clients (" + count + "): " + String.join(", ", clients.keySet());
    }

    private static boolean sendToClient(String name, String message) {
        ClientHandler target = clients.get(name);
        if (target == null) return false;
        target.send(message);
        return true;
    }

    private static class ClientHandler extends Thread {
        private final Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        private String clientName;

        ClientHandler(Socket socket) { this.socket = socket; }

        @Override
        public void run() {
            try {
                in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // unique name
                while (true) {
                    out.println("Enter your name:");
                    String name = in.readLine();
                    if (name == null) return;

                    name = name.trim();
                    if (name.isEmpty()) {
                        out.println("SERVER: Name cannot be empty.");
                        continue;
                    }
                    if (clients.putIfAbsent(name, this) != null) {
                        out.println("SERVER: Name already taken. Try another.");
                        continue;
                    }
                    clientName = name;
                    break;
                }

                System.out.println(clientName + " connected from " + socket.getInetAddress());
                out.println("SERVER: Welcome " + clientName + "!");
                out.println("SERVER: Private message format: @name message");
                out.println("SERVER: Type 'list' to see active clients. Type 'bye' to exit.");

                String msg;
                while ((msg = in.readLine()) != null) {
                    msg = msg.trim();
                    if (msg.isEmpty()) continue;

                    if (msg.equalsIgnoreCase("bye") || msg.equalsIgnoreCase("exit")) {
                        out.println("SERVER: Bye " + clientName);
                        break;
                    }

                    if (msg.equalsIgnoreCase("list")) {
                        out.println("SERVER: " + getClientSummary());
                        continue;
                    }

                    // server console log
                    System.out.println(clientName + " says: " + msg);

                    // ✅ relay to another client if message starts with @
                    if (msg.startsWith("@")) {
                        int space = msg.indexOf(' ');
                        if (space == -1) {
                            out.println("SERVER: Format: @name message");
                            continue;
                        }

                        String to = msg.substring(1, space).trim();
                        String text = msg.substring(space + 1).trim();

                        if (to.isEmpty() || text.isEmpty()) {
                            out.println("SERVER: Format: @name message");
                            continue;
                        }

                        boolean ok = sendToClient(to, "(Private) " + clientName + ": " + text);
                        if (!ok) out.println("SERVER: User '" + to + "' not found. Type 'list'.");
                        else out.println("SERVER: Sent to " + to);

                    } else {
                        // otherwise it's for the server only
                        out.println("SERVER: I received -> " + msg);
                    }
                }

            } catch (IOException e) {
                System.out.println("Client disconnected: " + (clientName != null ? clientName : socket.getInetAddress()));
            } finally {
                cleanup();
            }
        }

        void send(String message) {
            if (out != null) out.println(message);
        }

        void cleanup() {
            if (clientName != null) {
                clients.remove(clientName, this);
                System.out.println(clientName + " removed.");
            }
            try { socket.close(); } catch (IOException ignored) {}
        }
    }
}
