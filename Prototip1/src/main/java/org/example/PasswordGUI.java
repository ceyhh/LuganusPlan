package org.example;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class PasswordGUI extends JFrame {
    public static void StartGUI() {
        JFrame frame = new JFrame("Luganus Plan(for now)");
        JLabel label = new JLabel("Please enter your password and username");
        JLabel label3 = new JLabel("Password: ");
        JLabel label2 = new JLabel("Username: ");
        JLabel label4 = new JLabel();
        JButton button = new JButton("Login");
        JPasswordField password = new JPasswordField();
        JTextField username = new JTextField();

        label4.setBounds(300,100,100,30);
        button.setBounds(100, 200, 100, 30);
        label.setBounds(10, 10, 300, 20);
        label2.setBounds(10, 80, 80, 20);
        label3.setBounds(10, 140, 80, 20);
        password.setBounds(100, 140, 100, 20);
        username.setBounds(100, 80, 100, 20);

        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String usernameText = username.getText().trim();
                String passwordText = new String(password.getPassword()).trim();
                boolean found = false;

                File userFile = new File("kullanicilar.txt");
                if (!userFile.exists()) {
                    try {
                        userFile.createNewFile();
                    } catch (IOException ex) {
                        JOptionPane.showMessageDialog(frame, "User file error!");
                        System.exit(0);
                    }
                }

                String hashedPassword = hashPassword(passwordText);

                try (BufferedReader br = new BufferedReader(new FileReader(userFile))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        String[] parts = line.split(":");
                        if (parts.length == 2 && parts[0].equals(usernameText) && parts[1].equals(hashedPassword)) {
                            found = true;
                            break;
                        }
                    }
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(frame, "File read error!");
                    System.exit(0);
                }

                if (found) {
                    frame.dispose();
                    MessageBoxGUI.StartGUI(usernameText);
                } else {
                    JOptionPane.showMessageDialog(frame, "You are not registered, please register.");
                    System.exit(0);
                }
            }
        });

        frame.add(button);
        frame.add(label);
        frame.add(label3);
        frame.add(password);
        frame.add(label2);
        frame.add(username);

        frame.setSize(500, 500);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setLayout(null);
        frame.setVisible(true);
    }

    private static String hashPassword(String password) {
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
}
