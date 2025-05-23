package org.example;

import javax.net.ssl.SSLSocket;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

public class ClientHandler implements Runnable {
    private SSLSocket socket;
    private List<ClientHandler> clients;
    private PrintWriter out;
    private String clientName;
    private static final CopyOnWriteArraySet<String> onlineUsers = new CopyOnWriteArraySet<>();

    public ClientHandler(SSLSocket socket, List<ClientHandler> clients) {
        this.socket = socket;
        this.clients = clients;
    }

    public void sendMessage(String message) {
        out.println(message);
    }

    public void sendClientList() {
        String names = onlineUsers.stream().collect(Collectors.joining(", "));
        out.println("Connected clients: " + names);
    }

    public void sendHistory() {
        for (String msg : SSLServer.messageHistory) {
            // Mesaj zaten timestamp ile kaydedildiği için doğrudan gönder
            if (msg.contains(clientName + ": ")) {
                out.println(msg.replaceFirst(clientName + ": ", "You: "));
            } else {
                out.println(msg);
            }
        }
    }

    private String addTimestamp(String msg) {
        // Eğer mesajda zaten timestamp varsa tekrar ekleme
        if (msg.matches("^\\[\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\].*")) {
            return msg;
        }
        String timestamp = "[" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "]";
        return timestamp + " " + msg;
    }

    private boolean isUserStillRegistered() {
        File userFile = new File("kullanicilar.txt");
        if (!userFile.exists()) return false;
        try (BufferedReader br = new BufferedReader(new FileReader(userFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length >= 1 && parts[0].equals(clientName)) {
                    return true;
                }
            }
        } catch (IOException ignored) {
        }
        return false;
    }

    @Override
    public void run() {
        try {
            out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            out.println("Please enter your name:");
            clientName = in.readLine();

            // Eğer kullanıcı zaten online ise bağlantıyı reddet
            if (onlineUsers.contains(clientName)) {
                out.println("ERROR: This user is already active. Please try again later.");
                socket.close();
                return;
            }

            // Kullanıcı online listesine eklensin
            onlineUsers.add(clientName);

            for (ClientHandler client : clients) {
                client.sendClientList();
            }

            sendHistory();

            // Kullanıcı silinmiş mi kontrolü için thread başlat
            Thread registrationChecker = new Thread(() -> {
                try {
                    while (!socket.isClosed()) {
                        Thread.sleep(5000);
                        if (!isUserStillRegistered()) {
                            out.println("DISCONNECT: Your account has been deleted.");
                            socket.close();
                            break;
                        }
                    }
                } catch (Exception ignored) {
                }
            });
            registrationChecker.setDaemon(true);
            registrationChecker.start();

            while (true) {
                String inputLine = in.readLine();
                if (inputLine == null || inputLine.equalsIgnoreCase("Bye.")) break;

                // Her mesajda da kontrol et
                if (!isUserStillRegistered()) {
                    out.println("DISCONNECT: Your account has been deleted.");
                    break;
                }

                String msg = clientName + ": " + inputLine;
                String msgWithTime = addTimestamp(msg);
                System.out.println(msgWithTime);

                SSLServer.appendMessageToFile(msgWithTime);

                for (ClientHandler client : clients) {
                    if (client == this) {
                        client.sendMessage(msgWithTime.replaceFirst(clientName + ": ", "You: "));
                    } else {
                        client.sendMessage(msgWithTime);
                    }
                }
            }
            socket.close();
            clients.remove(this);

            // Kullanıcı online listesinden çıkarılsın
            onlineUsers.remove(clientName);

            for (ClientHandler client : clients) {
                client.sendClientList();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

       