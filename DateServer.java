import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public class DateServer {

    private static final int PORT = 6013;

    // name -> handler
    private static final ConcurrentHashMap<String, ClientHandler> clientsByName = new ConcurrentHashMap<>();

    // Who the server is currently chatting with (selected client)
    private static final AtomicReference<String> activeChat = new AtomicReference<>(null);

    public static void main(String[] args) {
        System.out.println("Server starting on port " + PORT + " ...");

        try (ServerSocket server = new ServerSocket(PORT);
             Scanner sc = new Scanner(System.in)) {

            // Accept clients continuously
            Thread acceptThread = new Thread(() -> {
                while (true) {
                    try {
                        Socket socket = server.accept();
                        new ClientHandler(socket).start();
                    } catch (IOException e) {
                        System.out.println("Accept error: " + e.getMessage());
                        break;
                    }
                }
            });
            acceptThread.setDaemon(true);
            acceptThread.start();

            printHelp();

            // SERVER CONSOLE = NORMAL CHAT INPUT
            while (true) {
                String line = sc.nextLine();
                if (line == null) continue;
                line = line.trim();
                if (line.isEmpty()) continue;

                // Slash commands
                if (line.equalsIgnoreCase("/help")) {
                    printHelp();
                    continue;
                }
                if (line.equalsIgnoreCase("/clients")) {
                    listClients();
                    continue;
                }
                if (line.toLowerCase().startsWith("/to ")) {
                    String name = line.substring(4).trim();
                    if (clientsByName.containsKey(name)) {
                        activeChat.set(name);
                        System.out.println("Now chatting with: " + name);
                        sendTo(name, "SERVER: (You are now chatting with the server)");
                    } else {
                        System.out.println("No such client: " + name);
                    }
                    continue;
                }
                if (line.toLowerCase().startsWith("/all ")) {
                    String msg = line.substring(5).trim();
                    broadcast("SERVER: " + msg, null);
                    continue;
                }
                if (line.equalsIgnoreCase("/exit")) {
                    System.out.println("Shutting down server...");
                    shutdownAll();
                    break;
                }

                // NORMAL CHAT BEHAVIOR:
                // If active client selected -> send to them
                // Else -> broadcast (or you can choose to require /to)
                String target = activeChat.get();
                if (target != null && clientsByName.containsKey(target)) {
                    sendTo(target, "SERVER: " + line);
                    System.out.println("You -> " + target + ": " + line);
                } else {
                    // No active chat: broadcast to everyone
                    broadcast("SERVER: " + line, null);
                    System.out.println("You (broadcast): " + line);
                }
            }

        } catch (IOException e) {
            System.out.println("Server error: " + e.getMessage());
        }
    }

    private static void printHelp() {
        System.out.println("\nServer chat commands:");
        System.out.println("  /clients              -> list connected client names");
        System.out.println("  /to <name>            -> chat with one client (active chat)");
        System.out.println("  /all <message>        -> broadcast to all clients");
        System.out.println("  /help                 -> show commands");
        System.out.println("  /exit                 -> shutdown server");
        System.out.println("\nNormal typing sends to active client; if none selected, it broadcasts.\n");
    }

    private static void listClients() {
        if (clientsByName.isEmpty()) {
            System.out.println("No clients connected.");
            return;
        }
        System.out.println("Connected clients: " + clientsByName.keySet());
        System.out.println("Active chat: " + activeChat.get());
    }

    private static void broadcast(String message, String excludeName) {
        for (ClientHandler ch : clientsByName.values()) {
            if (excludeName == null || !excludeName.equalsIgnoreCase(ch.clientName)) {
                ch.send(message);
            }
        }
    }

    private static void sendTo(String clientName, String message) {
        ClientHandler ch = clientsByName.get(clientName);
        if (ch == null) {
            System.out.println("No such client: " + clientName);
            return;
        }
        ch.send(message);
    }

    private static void shutdownAll() {
        for (ClientHandler ch : clientsByName.values()) {
            ch.close();
        }
        clientsByName.clear();
    }

    // Handles one client connection
    private static class ClientHandler extends Thread {
        private final Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        private String clientName;

        ClientHandler(Socket socket) throws IOException {
            this.socket = socket;
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.out = new PrintWriter(socket.getOutputStream(), true);
        }

        @Override
        public void run() {
            try {
                // Ask for name and ensure unique
                while (true) {
                    send("SERVER: Enter your name:");
                    String name = in.readLine();
                    if (name == null) return;

                    name = name.trim();
                    if (name.isEmpty()) {
                        send("SERVER: Name cannot be empty.");
                        continue;
                    }

                    ClientHandler existing = clientsByName.putIfAbsent(name, this);
                    if (existing != null) {
                        send("SERVER: Name already taken. Try another.");
                        continue;
                    }

                    clientName = name;
                    break;
                }

                send("SERVER: Welcome " + clientName + "! You are connected.");
                System.out.println(clientName + " connected from " + socket.getInetAddress());

                activeChat.compareAndSet(null, clientName);

                broadcast("SERVER: " + clientName + " joined.", clientName);

                String msg;
                while ((msg = in.readLine()) != null) {
                    msg = msg.trim();
                    if (msg.isEmpty()) continue;

                    System.out.println(clientName + ": " + msg);

                  
                    activeChat.set(clientName);

                    if (msg.equalsIgnoreCase("bye") || msg.equalsIgnoreCase("exit")) {
                        send("SERVER: Bye " + clientName);
                        break;
                    }

                  
                    send("SERVER: (received)");
                }

            } catch (IOException e) {
                System.out.println("Client disconnected: " + (clientName != null ? clientName : socket.getInetAddress()));
            } finally {
                close();
            }
        }

        void send(String message) {
            out.println(message);
        }

        void close() {
            if (clientName != null) {
                clientsByName.remove(clientName, this);
                broadcast("SERVER: " + clientName + " left.", clientName);
                System.out.println(clientName + " removed.");

               
                if (clientName.equals(activeChat.get())) {
                    String next = clientsByName.keySet().stream().findFirst().orElse(null);
                    activeChat.set(next);
                    if (next != null) {
                        System.out.println("Active chat moved to: " + next);
                    } else {
                        System.out.println("No active chat (no clients).");
                    }
                }
            }
            try { socket.close(); } catch (IOException ignored) {}
        }
    }
}
