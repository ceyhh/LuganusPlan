package org.example;

import javax.net.ssl.*;
import javax.swing.*;
import java.io.*;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class SSLClient {
    private PrintWriter out;
    private BufferedReader in;
    private String userName;
    private String userPassword; // kullanıcıdan alınan şifre
    private MessageBoxGUI gui;
    private SSLSocket kkSocket; // Bağlantıyı kapatmak için referans
    private String serverIp;
    private int serverPort;

    public SSLClient(MessageBoxGUI gui, String userName, String userPassword, String serverIp, int serverPort) {
        this.gui = gui;
        this.userName = userName;
        this.userPassword = userPassword;
        this.serverIp = serverIp;
        this.serverPort = serverPort;
    }

    // Eski constructor (kullanılmayacak)
    public SSLClient(MessageBoxGUI gui, String userName) {
        this(gui, userName, null, "127.0.0.1", 8333);
    }

    public void connectAndStart() {
        try {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            String certPassword = "aabbcc";
            InputStream inputStream = ClassLoader.getSystemClassLoader().getResourceAsStream("client-certificate.p12");
            keyStore.load(inputStream, certPassword.toCharArray());

            // TrustManager'ı geçici olarak tüm sertifikalara güvenecek şekilde değiştiriyoruz
            TrustManager[] trustAllCerts = new TrustManager[] {
                    new X509TrustManager() {
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return null;
                        }
                        public void checkClientTrusted(X509Certificate[] certs, String authType) {
                        }
                        public void checkServerTrusted(X509Certificate[] certs, String authType) {
                        }
                    }
            };

            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509", "SunJSSE");
            keyManagerFactory.init(keyStore, certPassword.toCharArray());
            X509KeyManager x509KeyManager = null;
            for (KeyManager keyManager : keyManagerFactory.getKeyManagers()) {
                if (keyManager instanceof X509KeyManager) {
                    x509KeyManager = (X509KeyManager) keyManager;
                    break;
                }
            }
            if (x509KeyManager == null) throw new NullPointerException();

            SSLContext sslContext = SSLContext.getInstance("TLS");
            // trustAllCerts'i kullanarak SSLContext'i başlatıyoruz
            sslContext.init(new KeyManager[]{x509KeyManager}, trustAllCerts, null);

            SSLSocketFactory socketFactory = sslContext.getSocketFactory();
            kkSocket = (SSLSocket) socketFactory.createSocket(serverIp, serverPort);
            kkSocket.setEnabledProtocols(new String[]{"TLSv1.2"});
            // Desteklenen tüm şifreleme paketlerini ayarlayın
            kkSocket.setEnabledCipherSuites(kkSocket.getSupportedCipherSuites());

            out = new PrintWriter(kkSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(kkSocket.getInputStream()));

            // Kullanıcı adı ve şifreyi düz olarak gönder
            String prompt = in.readLine();
            if (prompt != null && prompt.equals("AUTH_REQUEST")) {
                out.println(userName);
                out.println(userPassword);

                // Sunucudan auth cevabını bekle
                String authResp = in.readLine();
                if ("auth_OK".equals(authResp)) {
                    // giriş başarılı, devam et
                } else if ("auth_NOTOK".equals(authResp)) {
                    JOptionPane.showMessageDialog(null, "Authentication failed!", "Login Error", JOptionPane.ERROR_MESSAGE);
                    System.exit(0);
                } else {
                    JOptionPane.showMessageDialog(null, "Other problems occurred during authentication.", "Login Error", JOptionPane.ERROR_MESSAGE);
                    System.exit(0);
                }
            }

            Thread readerThread = new Thread(() -> {
                String fromServer;
                try {
                    while ((fromServer = in.readLine()) != null) {
                        gui.appendMessage(fromServer);
                    }
                } catch (IOException e) {
                    gui.appendMessage("Connection closed.");
                }
            });
            readerThread.start();
        } catch (Exception e) {
            gui.appendMessage("Connection error: " + e.getMessage());
        }
    }

    public void sendMessage(String msg) {
        if (out != null) {
            out.println(msg);
        }
    }

    public void disconnect() {
        try {
            if (out != null) out.println("Bye.");
            if (kkSocket != null) kkSocket.close();
        } catch (IOException ignored) {}
    }
}


