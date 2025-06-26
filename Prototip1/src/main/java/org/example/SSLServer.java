package org.example;

import javax.net.ssl.*;
import java.io.*;
import java.security.*;
import java.security.cert.CertificateException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class SSLServer {
    // Mesajların kaydedileceği dosya yolu
    public static final String MESSAGE_LOG_FILE = "chatlog.txt";
    // Tüm eski mesajlar burada tutulacak
    public static List<String> messageHistory = new CopyOnWriteArrayList<>();
    public static ServerGUI serverGUIInstance = null;

    public static void main(String[] args) {
        // Server GUI\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\'yi başlat
        javax.swing.SwingUtilities.invokeLater(() -> {
            serverGUIInstance = new ServerGUI();
            serverGUIInstance.setVisible(true);
        });

        List<ClientHandler> clients = new CopyOnWriteArrayList<>();
        // Sunucu başlarken eski mesajları oku
        loadMessageHistory();

        try {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            String password = "abcdefg";
            InputStream inputStream = ClassLoader.getSystemClassLoader().getResourceAsStream("server-certificate.p12");
            keyStore.load(inputStream, password.toCharArray());

            String trustPassword = "abcdefg";
            KeyStore trustStore = KeyStore.getInstance("PKCS12");
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            InputStream inputStream1 = ClassLoader.getSystemClassLoader().getResourceAsStream("server-truststore.p12");
            trustStore.load(inputStream1, trustPassword.toCharArray());
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

            SSLServerSocketFactory serverSocketFactory = sslContext.getServerSocketFactory();
            SSLServerSocket serverSocket = (SSLServerSocket) serverSocketFactory.createServerSocket(8333);
            serverSocket.setNeedClientAuth(true);
            serverSocket.setEnabledProtocols(new String[]{"TLSv1.2"});
            // Desteklenen tüm şifreleme paketlerini ayarlayın
            serverSocket.setEnabledCipherSuites(serverSocket.getSupportedCipherSuites());

            System.out.println("Server started, waiting for clients...");

            while (true) {
                SSLSocket socket = (SSLSocket) serverSocket.accept();
                System.out.println("A client connected.");
                ClientHandler handler = new ClientHandler(socket, clients);
                clients.add(handler);
                new Thread(handler).start();
            }
        } catch (IOException | CertificateException | NoSuchAlgorithmException | UnrecoverableKeyException |
                 NoSuchProviderException | KeyStoreException | KeyManagementException e) {
            e.printStackTrace();
        }
    }

    // Eski mesajları dosyadan oku
    private static void loadMessageHistory() {
        File file = new File(MESSAGE_LOG_FILE);
        if (!file.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                messageHistory.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Yeni mesajı dosyaya ekle ve GUI\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\'ye logla
    public static synchronized void appendMessageToFile(String message) {
        messageHistory.add(message);
        try (FileWriter fw = new FileWriter(MESSAGE_LOG_FILE, true);
             BufferedWriter bw = new BufferedWriter(fw)) {
            bw.write(message);
            bw.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // Server GUI\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\'ye anlık log ekle
        if (serverGUIInstance != null) {
            // Eğer mesaj zaten timestamp ile başlıyorsa tekrar ekleme
            if (message.matches("^\\[\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\] .*")) {
                serverGUIInstance.appendLog(message);
            } else {
                String timestamp = "[" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "]";
                serverGUIInstance.appendLog(timestamp + " " + message);
            }
        }
    }
}


