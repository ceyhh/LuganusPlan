package org.example;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;

public class MessageBoxGUI extends JFrame {
    private JTextArea messageArea;
    private JTextField inputField;
    private JButton sendButton;
    private SSLClient client;
    private String userName;

    // Kullanıcı listesi için eklenen alanlar
    private DefaultListModel<String> userListModel;
    private JList<String> userList;
    private Map<String, Boolean> userStatusMap = new HashMap<>(); // kullanıcı adı -> online mı

    public MessageBoxGUI(String userName) {
        this.userName = userName;
        setTitle("Chat Box - " + userName);
        setSize(700, 500);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE); // Kapatma işlemini kendimiz yöneteceğiz
        setLocationRelativeTo(null);

        messageArea = new JTextArea();
        messageArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(messageArea);

        inputField = new JTextField();
        sendButton = new JButton("Send");

        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        // Kullanıcı listesi paneli
        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        userList.setCellRenderer(new UserStatusRenderer());
        JScrollPane userListScroll = new JScrollPane(userList);
        userListScroll.setPreferredSize(new Dimension(180, 0));
        loadAllUsers();

        // Layout
        setLayout(new BorderLayout());
        add(scrollPane, BorderLayout.CENTER);
        add(inputPanel, BorderLayout.SOUTH);
        add(userListScroll, BorderLayout.EAST);

        // SSLClient başlat
        client = new SSLClient(this, userName);
        client.connectAndStart();

        // Mesaj gönderme
        sendButton.addActionListener(e -> sendMessage());
        inputField.addActionListener(e -> sendMessage());

        // Pencere kapatılırken bağlantıyı düzgün kapat
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                if (client != null) {
                    client.disconnect();
                }
                dispose();
                System.exit(0);
            }
        });
    }

    // Kayıtlı kullanıcıları txt'den oku ve offline olarak başlat
    private void loadAllUsers() {
        userStatusMap.clear();
        userListModel.clear();
        File userFile = new File("kullanicilar.txt");
        if (userFile.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(userFile))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] parts = line.split(":");
                    if (parts.length >= 1 && !parts[0].isEmpty()) {
                        userStatusMap.put(parts[0], false);
                    }
                }
            } catch (IOException ignored) {}
        }
        updateUserList();
    }

    // Sunucudan gelen "Connected clients: ..." mesajı ile online kullanıcıları güncelle
    public void updateOnlineUsers(String connectedLine) {
        // örnek: Connected clients: niga, testuser
        Set<String> online = new HashSet<>();
        if (connectedLine.startsWith("Connected clients:")) {
            String users = connectedLine.substring("Connected clients:".length()).trim();
            if (!users.isEmpty()) {
                for (String u : users.split(",")) {
                    online.add(u.trim());
                }
            }
        }
        for (String user : userStatusMap.keySet()) {
            userStatusMap.put(user, online.contains(user));
        }
        updateUserList();
    }

    // Kullanıcı listesini güncelle
    private void updateUserList() {
        SwingUtilities.invokeLater(() -> {
            userListModel.clear();
            for (String user : userStatusMap.keySet()) {
                boolean online = userStatusMap.get(user);
                userListModel.addElement(user + (online ? " (Online)" : " (Offline)"));
            }
        });
    }

    public void appendMessage(String message) {
        // Kullanıcı listesi mesajı geldiyse kullanıcı listesini güncelle
        if (message.startsWith("Connected clients:")) {
            updateOnlineUsers(message);
            return;
        }
        SwingUtilities.invokeLater(() -> {
            // Mesajdan saat ve kullanıcı adını ayıkla
            // Beklenen format: [yyyy-MM-dd HH:mm:ss] clientname: mesaj
            String displayMsg = message;
            try {
                if (message.startsWith("[")) {
                    int closeIdx = message.indexOf("]");
                    if (closeIdx > 0 && message.length() > closeIdx + 2) {
                        String datetime = message.substring(1, closeIdx); // yyyy-MM-dd HH:mm:ss
                        String hourMinute = datetime.substring(11, 16);   // HH:mm
                        String rest = message.substring(closeIdx + 2);    // clientname: mesaj
                        displayMsg = hourMinute + " " + rest;
                    }
                }
            } catch (Exception ex) {
                // Hatalı formatta ise olduğu gibi göster
                displayMsg = message;
            }
            messageArea.append(displayMsg + "\n");
            messageArea.setCaretPosition(messageArea.getDocument().getLength());
        });
    }

    private void sendMessage() {
        String text = inputField.getText().trim();
        if (!text.isEmpty()) {
            client.sendMessage(text);
            inputField.setText("");
        }
    }

    public static void StartGUI(String userName) {
        SwingUtilities.invokeLater(() -> {
            MessageBoxGUI gui = new MessageBoxGUI(userName);
            gui.setVisible(true);
        });
    }

    // Kullanıcı listesi için özel renderer (online yeşil, offline gri)
    private static class UserStatusRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            String val = value.toString();
            if (val.endsWith("(Online)")) {
                label.setForeground(new Color(0, 153, 0));
            } else {
                label.setForeground(Color.GRAY);
            }
            return label;
        }
    }
}
