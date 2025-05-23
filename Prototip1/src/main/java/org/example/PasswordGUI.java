package org.example;
import javax.net.ssl.*;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.security.KeyStore;

public class PasswordGUI extends JFrame {
    public static void StartGUI() {
        // 1. Sunucu IP ve portunu iste
        JTextField ipField = new JTextField("127.0.0.1");
        JTextField portField = new JTextField("8333");
        Object[] fields = {
            "Server IP:", ipField,
            "Server Port:", portField
        };
        int option = JOptionPane.showConfirmDialog(null, fields, "Enter Server IP and Port", JOptionPane.OK_CANCEL_OPTION);
        if (option != JOptionPane.OK_OPTION) {
            System.exit(0);
        }
        final String serverIp = ipField.getText().trim();
        final int serverPort;
        int tempPort = 8333;
        try {
            tempPort = Integer.parseInt(portField.getText().trim());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, "Invalid port. Using 8333.");
        }
        serverPort = tempPort;

        // 2. Firewall açma kodu (Windows için)
        try {
            String command = "netsh advfirewall firewall add rule name=\"LuganusChatClient\" dir=out action=allow protocol=TCP localport=" + serverPort;
            Process p = Runtime.getRuntime().exec(command);
            p.waitFor();
        } catch (Exception ex) {
            // Hata olursa sessiz geç
        }

        // 3. Bağlantı test et
        boolean canConnect = false;
        try {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            String password = "aabbcc";
            InputStream inputStream = ClassLoader.getSystemClassLoader().getResourceAsStream("client-certificate.p12");
            keyStore.load(inputStream, password.toCharArray());

            KeyStore trustStore = KeyStore.getInstance("PKCS12");
            String password2 = "abcdefg";
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            InputStream inputStream1 = ClassLoader.getSystemClassLoader().getResourceAsStream("server-certificate.p12");
            trustStore.load(inputStream1, password2.toCharArray());
            trustManagerFactory.init(trustStore);
            X509TrustManager x509TrustManager = null;
            for (TrustManager trustManager : trustManagerFactory.getTrustManagers()) {
                if (trustManager instanceof X509TrustManager) {
                    x509TrustManager = (X509TrustManager) trustManager;
                    break;
                }
            }
            if (x509TrustManager == null) throw new NullPointerException();

            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509", "SunJSSE");
            keyManagerFactory.init(keyStore, password.toCharArray());
            X509KeyManager x509KeyManager = null;
            for (KeyManager keyManager : keyManagerFactory.getKeyManagers()) {
                if (keyManager instanceof X509KeyManager) {
                    x509KeyManager = (X509KeyManager) keyManager;
                    break;
                }
            }
            if (x509KeyManager == null) throw new NullPointerException();

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(new KeyManager[]{x509KeyManager}, new TrustManager[]{x509TrustManager}, null);

            SSLSocketFactory socketFactory = sslContext.getSocketFactory();
            SSLSocket testSocket = (SSLSocket) socketFactory.createSocket(serverIp, serverPort);
            testSocket.setEnabledProtocols(new String[]{"TLSv1.2"});
            testSocket.close();
            canConnect = true;
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, "Cannot connect to server at " + serverIp + ":" + serverPort + "\n" + ex.getMessage());
            System.exit(0);
        }

        if (canConnect) {
            // 4. Kullanıcı adı/şifre ekranı aç
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
                        MessageBoxGUI.StartGUI(usernameText, serverIp, serverPort);
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
    }

    private static String hashPassword(String password) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }
}
