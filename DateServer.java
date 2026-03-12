import java.io.*;
import java.net.*;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

public class DateServer {

    private static final String SERVER_IP = "0.0.0.0"; // bind to all interfaces
    private static final int PORT = 6013;

    // Directory: client name -> handler
    private static final ConcurrentHashMap<String, ClientHandler> clients = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        System.out.println("Server Starting...");
        System.out.println("IP   : " + SERVER_IP);
        System.out.println("PORT : " + PORT);

        startServerConsole();

        try (ServerSocket serverSocket = new ServerSocket()) {
            serverSocket.bind(new InetSocketAddress(SERVER_IP, PORT));
            System.out.println("Server is running and waiting for clients...");

            while (true) {
                Socket socket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(socket);
                clientHandler.start();
            }
        } catch (IOException e) {
            System.out.println("Server error: " + e.getMessage());
        }
    }

    // -------------------- SERVER CONSOLE --------------------
    private static void startServerConsole() {
        Thread consoleThread = new Thread(() -> {
            Scanner scanner = new Scanner(System.in);

            System.out.println("\nSERVER CONSOLE READY");
            System.out.println("Commands:");
            System.out.println("list");
            System.out.println("to <clientName> <message>");
            System.out.println("broadcast <message>");
            System.out.println("kick <clientName>");
            System.out.println("------------------------------------");

            while (true) {
                try {
                    String line = scanner.nextLine().trim();

                    if (line.equalsIgnoreCase("list")) {
                        System.out.println(getDetailedClientList());
                    } else if (line.toLowerCase().startsWith("to ")) {
                        String[] parts = line.split("\\s+", 3);
                        if (parts.length < 3) {
                            System.out.println("Usage: to <clientName> <message>");
                            continue;
                        }

                        String targetName = parts[1];
                        String message = parts[2];

                        if (sendToClient(targetName, "SERVER (private): " + message)) {
                            System.out.println("Message sent to " + targetName);
                        } else {
                            System.out.println("Client not found: " + targetName);
                        }

                    } else if (line.toLowerCase().startsWith("broadcast ")) {
                        String message = line.substring("broadcast ".length()).trim();
                        if (message.isEmpty()) {
                            System.out.println("Usage: broadcast <message>");
                            continue;
                        }

                        broadcast("SERVER (broadcast): " + message);
                        System.out.println("Broadcast sent.");

                    } else if (line.toLowerCase().startsWith("kick ")) {
                        String[] parts = line.split("\\s+", 2);
                        if (parts.length < 2 || parts[1].trim().isEmpty()) {
                            System.out.println("Usage: kick <clientName>");
                            continue;
                        }

                        String targetName = parts[1].trim();
                        if (forceCloseClient(targetName)) {
                            System.out.println("Client kicked: " + targetName);
                        } else {
                            System.out.println("Client not found: " + targetName);
                        }

                    } else {
                        System.out.println("Unknown command.");
                    }
                } catch (Exception e) {
                    System.out.println("Console error: " + e.getMessage());
                }
            }
        });

        consoleThread.setDaemon(true);
        consoleThread.start();
    }

    // -------------------- HELPER METHODS --------------------
    private static void broadcast(String message) {
        for (Map.Entry<String, ClientHandler> entry : clients.entrySet()) {
            entry.getValue().send(message);
        }
    }

    private static boolean sendToClient(String clientName, String message) {
        ClientHandler client = clients.get(clientName);
        if (client == null) return false;
        client.send(message);
        return true;
    }

    private static boolean forceCloseClient(String clientName) {
        ClientHandler client = clients.get(clientName);
        if (client == null) return false;

        client.send("SERVER: You have been forcefully disconnected by the server.");
        client.closeConnection();
        return true;
    }

    private static String getSimpleClientList() {
        if (clients.isEmpty()) {
            return "No active clients.";
        }

        StringBuilder sb = new StringBuilder("Active clients: ");
        boolean first = true;
        for (String name : clients.keySet()) {
            if (!first) sb.append(", ");
            sb.append(name);
            first = false;
        }
        return sb.toString();
    }

    private static String getDetailedClientList() {
        if (clients.isEmpty()) {
            return "No active clients.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\n========== CLIENT DIRECTORY ==========\n");

        for (Map.Entry<String, ClientHandler> entry : clients.entrySet()) {
            ClientHandler client = entry.getValue();
            sb.append("Client Name : ").append(client.getClientName()).append("\n");
            sb.append("Client IP   : ").append(client.getClientIP()).append("\n");
            sb.append("Thread Name : ").append(client.getName()).append("\n");
            sb.append("--------------------------------------\n");
        }

        return sb.toString();
    }

    private static void sendUpdatedClientListToAll() {
        broadcast("SERVER: Updated Client List -> " + getSimpleClientList());
    }

    // -------------------- CLIENT HANDLER --------------------
    private static class ClientHandler extends Thread {
        private final Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        private String clientName;
        private String clientIP;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public String getClientName() {
            return clientName;
        }

        public String getClientIP() {
            return clientIP;
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
                clientIP = socket.getInetAddress().getHostAddress();

                // Ask for unique client name
                while (true) {
                    out.println("SERVER: Enter your name:");
                    String name = in.readLine();

                    if (name == null) return;

                    name = name.trim();

                    if (name.isEmpty()) {
                        out.println("SERVER: Name cannot be empty.");
                        continue;
                    }

                    if (clients.putIfAbsent(name, this) != null) {
                        out.println("SERVER: Name already exists. Enter another name.");
                        continue;
                    }

                    clientName = name;
                    break;
                }

                System.out.println(clientName + " connected from " + clientIP + " using thread " + getName());

                out.println("SERVER: Welcome " + clientName + "!");
                out.println("SERVER: Available commands:");
                out.println("list                    -> get active client list");
                out.println("@clientName message     -> send private message");
                out.println("all message             -> broadcast to everyone");
                out.println("exit                    -> leave chat");

                broadcast("SERVER: " + clientName + " joined the chat.");
                sendUpdatedClientListToAll();

                String message;
                while ((message = in.readLine()) != null) {
                    message = message.trim();

                    if (message.isEmpty()) continue;

                    if (message.equalsIgnoreCase("exit")) {
                        out.println("SERVER: Goodbye " + clientName);
                        break;
                    }

                    if (message.equalsIgnoreCase("list")) {
                        out.println("SERVER: " + getSimpleClientList());
                        continue;
                    }

                    // Broadcast request from client
                    if (message.toLowerCase().startsWith("all ")) {
                        String broadcastMessage = message.substring(4).trim();
                        if (broadcastMessage.isEmpty()) {
                            out.println("SERVER: Usage -> all <message>");
                        } else {
                            broadcast("(Broadcast) " + clientName + ": " + broadcastMessage);
                        }
                        continue;
                    }

                    // Private message to specific client
                    if (message.startsWith("@")) {
                        int firstSpace = message.indexOf(' ');
                        if (firstSpace == -1) {
                            out.println("SERVER: Usage -> @clientName <message>");
                            continue;
                        }

                        String targetName = message.substring(1, firstSpace).trim();
                        String privateMessage = message.substring(firstSpace + 1).trim();

                        if (targetName.isEmpty() || privateMessage.isEmpty()) {
                            out.println("SERVER: Usage -> @clientName <message>");
                            continue;
                        }

                        if (targetName.equalsIgnoreCase(clientName)) {
                            out.println("SERVER: You cannot send a message to yourself.");
                            continue;
                        }

                        boolean sent = sendToClient(targetName, "(Private) " + clientName + ": " + privateMessage);
                        if (sent) {
                            out.println("SERVER: Message sent to " + targetName);
                        } else {
                            out.println("SERVER: Client '" + targetName + "' not found.");
                        }
                        continue;
                    }

                    // Normal message to server
                    System.out.println(clientName + " says to server: " + message);
                    out.println("SERVER: Message received -> " + message);
                }

            } catch (IOException e) {
                System.out.println("Connection lost with " + (clientName != null ? clientName : "unknown client"));
            } finally {
                cleanup();
            }
        }

        public void send(String message) {
            if (out != null) {
                out.println(message);
            }
        }

        public void closeConnection() {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }

        private void cleanup() {
            if (clientName != null) {
                boolean removed = clients.remove(clientName, this);
                if (removed) {
                    System.out.println(clientName + " disconnected and removed.");
                    broadcast("SERVER: " + clientName + " left the chat.");
                    sendUpdatedClientListToAll();
                }
            }

            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }
}
