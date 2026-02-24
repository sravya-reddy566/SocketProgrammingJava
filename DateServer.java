import java.io.*;
import java.net.*;
import java.util.concurrent.ConcurrentHashMap;

public class DateServer {

    private static final int PORT = 6013;

    // Store clients by unique name (thread-safe)
    private static final ConcurrentHashMap<String, ClientHandler> clients = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        System.out.println("Server starting on port " + PORT + " ...");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Waiting for clients...");

            while (true) {
                Socket socket = serverSocket.accept();
                new ClientHandler(socket).start();
            }

        } catch (IOException e) {
            System.out.println("Server error: " + e.getMessage());
        }
    }

    // Broadcast a message to all clients except the sender (optional)
    private static void broadcast(String message, String excludeName) {
        for (ClientHandler ch : clients.values()) {
            if (excludeName == null || !excludeName.equalsIgnoreCase(ch.clientName)) {
                ch.send(message);
            }
        }
    }

    // Handles one client connection
    private static class ClientHandler extends Thread {
        private final Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        private String clientName;

        ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // 1) Ask for name and ensure unique
                while (true) {
                    out.println("SERVER: Enter your name:");
                    String name = in.readLine();

                    if (name == null) return; // disconnected before naming
                    name = name.trim();

                    if (name.isEmpty()) {
                        out.println("SERVER: Name cannot be empty.");
                        continue;
                    }

                    // Reject duplicates
                    ClientHandler existing = clients.putIfAbsent(name, this);
                    if (existing != null) {
                        out.println("SERVER: Name already taken. Try another.");
                        continue;
                    }

                    clientName = name;
                    break;
                }

                // 2) Store client info already done (map holds handler)
                System.out.println(clientName + " connected from " + socket.getInetAddress());
                out.println("SERVER: Welcome " + clientName + "! You are connected.");

                broadcast("SERVER: " + clientName + " joined.", clientName);

                // 3) Read messages and display them on server
                String msg;
                while ((msg = in.readLine()) != null) {
                    msg = msg.trim();
                    if (msg.isEmpty()) continue;

                    // REQUIRED: Server displays all messages from different clients
                    System.out.println(clientName + " says: " + msg);

                    if (msg.equalsIgnoreCase("exit") || msg.equalsIgnoreCase("bye")) {
                        out.println("SERVER: Bye " + clientName);
                        break;
                    }

                    // Optional: broadcast to other clients
                    broadcast(clientName + ": " + msg, clientName);
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
                broadcast("SERVER: " + clientName + " left.", clientName);
                System.out.println(clientName + " removed.");
            }
            try { socket.close(); } catch (IOException ignored) {}
        }
    }
}
