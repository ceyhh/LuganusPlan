package org.example;

import javax.net.ssl.*;
import java.io.*;
import java.security.*;
import java.security.cert.CertificateException;

public class SSLClient {
    private PrintWriter out;
    private BufferedReader in;
    private String userName;
    private MessageBoxGUI gui;
    private SSLSocket kkSocket; // Bağlantıyı kapatmak için referans

    public SSLClient(MessageBoxGUI gui, String userName) {
        this.gui = gui;
        this.userName = userName;
    }

    public void connectAndStart() {
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
            kkSocket = (SSLSocket) socketFactory.createSocket("127.0.0.1", 8333);
            kkSocket.setEnabledProtocols(new String[]{"TLSv1.2"});

            out = new PrintWriter(kkSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(kkSocket.getInputStream()));

            // Kullanıcı adını GUI'den alma kaldırıldı, doğrudan gönder
            String prompt = in.readLine();
            if (prompt != null && prompt.equals("Please enter your name:")) {
                out.println(userName);
            }

            // Mesajları GUI'ye aktaran thread
            Thread readerThread = new Thread(() -> {
                String fromServer;
                try {
                    while ((fromServer = in.readLine()) != null) {
                        // Eğer kullanıcı zaten aktifse hata kutucuğu göster ve çık
                        if (fromServer.startsWith("ERROR: This user is already active")) {
                            javax.swing.SwingUtilities.invokeLater(() -> {
                                javax.swing.JOptionPane.showMessageDialog(null,
                                        "This user is currently active. Please try again later.",
                                        "Login Error",
                                        javax.swing.JOptionPane.ERROR_MESSAGE);
                                System.exit(0);
                            });
                            break;
                        }
                        gui.appendMessage(fromServer);
                    }
                } catch (IOException e) {
                    gui.appendMessage("Connection closed.");
                }
            });
            readerThread.setDaemon(true);
            readerThread.start();

        } catch (Exception e) {
            gui.appendMessage("Connection error: " + e.getMessage());
        }
    }

    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }

    // Bağlantıyı düzgün kapat
    public void disconnect() {
        try {
            if (out != null) out.println("Bye.");
            if (kkSocket != null && !kkSocket.isClosed()) kkSocket.close();
        } catch (IOException ignored) {}
    }
}
