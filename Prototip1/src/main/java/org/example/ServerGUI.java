package org.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.time.*;
import java.util.List;

public class ServerGUI extends JFrame {
    private JTextArea logArea;
    private DefaultListModel<String> userListModel;
    private JList<String> userList;
    private Map<String, Boolean> userStatusMap = new HashMap<>();

    public ServerGUI() {
        setTitle("Server Management");
        setSize(800, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JButton registerButton = new JButton("Register User");
        registerButton.addActionListener(this::showRegisterDialog);

        JButton deleteButton = new JButton("Delete User");
        deleteButton.addActionListener(this::showDeleteDialog);

        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout());
        panel.add(registerButton);
        panel.add(deleteButton);

        logArea = new JTextArea();
        logArea.setEditable(false);
        JScrollPane logScroll = new JScrollPane(logArea);

        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        userList.setCellRenderer(new UserStatusRenderer());
        JScrollPane userListScroll = new JScrollPane(userList);
        userListScroll.setPreferredSize(new Dimension(200, 0));

        setLayout(new BorderLayout());
        add(panel, BorderLayout.NORTH);
        add(logScroll, BorderLayout.CENTER);
        add(userListScroll, BorderLayout.EAST);

        appendAllUsersToLog();
        appendAllMessagesToLog();
        loadAllUsers();
        updateUserList();

        // Online kullanıcılar eşzamanlı güncellensin
        new javax.swing.Timer(1000, evt -> {
            loadAllUsers();
            updateUserList();
        }).start();
    }

    private void showRegisterDialog(ActionEvent e) {
        JTextField usernameField = new JTextField();
        JPasswordField passwordField = new JPasswordField();
        Object[] fields = {
                "Username:", usernameField,
                "Password:", passwordField
        };
        int option = JOptionPane.showConfirmDialog(this, fields, "Register User", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword()).trim();
            if (username.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Username and password cannot be empty.");
                return;
            }
            if (isUserExists(username)) {
                JOptionPane.showMessageDialog(this, "This username is already registered.");
                return;
            }
            String hashed = hashPassword(password);
            if (hashed == null) {
                JOptionPane.showMessageDialog(this, "Hash error!");
                return;
            }
            try (FileWriter fw = new FileWriter("kullanicilar.txt", true);
                 BufferedWriter bw = new BufferedWriter(fw)) {
                bw.write(username + ":" + hashed);
                bw.newLine();
                JOptionPane.showMessageDialog(this, "User registered successfully.");
                appendLog(getTimestamp() + " Registered user: " + username);
                loadAllUsers();
                updateUserList();
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "File write error!");
            }
        }
    }

    private void showDeleteDialog(ActionEvent e) {
        String username = JOptionPane.showInputDialog(this, "Enter username to delete:");
        if (username == null || username.trim().isEmpty()) {
            return;
        }
        username = username.trim();
        if (!isUserExists(username)) {
            JOptionPane.showMessageDialog(this, "User not found.");
            return;
        }
        if (deleteUser(username)) {
            JOptionPane.showMessageDialog(this, "User deleted successfully.");
            appendLog(getTimestamp() + " Deleted user: " + username);
            loadAllUsers();
            updateUserList();
        } else {
            JOptionPane.showMessageDialog(this, "Error deleting user.");
        }
    }

    private boolean isUserExists(String username) {
        File userFile = new File("kullanicilar.txt");
        if (!userFile.exists()) return false;
        try (BufferedReader br = new BufferedReader(new FileReader(userFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length >= 1 && parts[0].equals(username)) {
                    return true;
                }
            }
        } catch (IOException ignored) {}
        return false;
    }

    private boolean deleteUser(String username) {
        File userFile = new File("kullanicilar.txt");
        File tempFile = new File("kullanicilar_temp.txt");
        boolean deleted = false;
        try (BufferedReader br = new BufferedReader(new FileReader(userFile));
             BufferedWriter bw = new BufferedWriter(new FileWriter(tempFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length >= 1 && parts[0].equals(username)) {
                    deleted = true;
                    continue;
                }
                bw.write(line);
                bw.newLine();
            }
        } catch (IOException e) {
            return false;
        }
        if (deleted) {
            if (!userFile.delete() || !tempFile.renameTo(userFile)) {
                return false;
            }
        } else {
            tempFile.delete();
        }
        return deleted;
    }

    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    private void appendAllUsersToLog() {
        File userFile = new File("kullanicilar.txt");
        if (!userFile.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(userFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length >= 1 && !parts[0].isEmpty()) {
                    appendLog(getTimestamp() + " Registered user: " + parts[0]);
                }
            }
        } catch (IOException ignored) {}
    }

    private void appendAllMessagesToLog() {
        File msgFile = new File("chatlog.txt");
        if (!msgFile.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(msgFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                // Mesaj zaten timestamp ile kaydedilmişse olduğu gibi göster
                appendLog(line);
            }
        } catch (IOException ignored) {}
    }

    public void appendLog(String log) {
        logArea.append(log + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    private String getTimestamp() {
        return "[" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "]";
    }

    // Kullanıcı listesini txt'den oku ve online durumunu güncelle
    private void loadAllUsers() {
        userStatusMap.clear();
        File userFile = new File("kullanicilar.txt");
        if (userFile.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(userFile))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] parts = line.split(":");
                    if (parts.length >= 1 && !parts[0].isEmpty()) {
                        userStatusMap.put(parts[0], isUserOnline(parts[0]));
                    }
                }
            } catch (IOException ignored) {}
        }
    }

    // Online kullanıcıları ClientHandler'ın onlineUsers setinden oku
    private boolean isUserOnline(String username) {
        try {
            java.lang.reflect.Field f = Class.forName("org.example.ClientHandler").getDeclaredField("onlineUsers");
            f.setAccessible(true);
            Set<?> onlineSet = (Set<?>) f.get(null);
            return onlineSet.contains(username);
        } catch (Exception e) {
            return false;
        }
    }

    private void updateUserList() {
        SwingUtilities.invokeLater(() -> {
            userListModel.clear();
            for (String user : userStatusMap.keySet()) {
                boolean online = userStatusMap.get(user);
                userListModel.addElement(user + (online ? " (Online)" : " (Offline)"));
            }
        });
    }

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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ServerGUI gui = new ServerGUI();
            gui.setVisible(true);
        });
    }
}
