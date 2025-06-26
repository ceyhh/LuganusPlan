# Luganus Plan
# A open source basic java chatting application

#### Thanks to Alpha Garden for SSL-TLS example [TLS EXAMPLE](https://github.com/AlphaGarden/SSL-Client-Server) I used his example to do TLS part.
#### This project has some AI tools usage such as Copilot and CHATGPT

## Properties of Project
### Client Side

# If you want to sent client side to other computers: 
- You must use your own certificates that have SNA's of your dna on it. Otherwise it wont work out.
- In the given certificate i used a sample ngrok dns to use.
- Never tested from other networks but works perfectly fine on local computer.
- Client side runs with Main.java in order to run it you should run Protoip1\target\Prototip1-1.0-SNAPSHOT.jar .
- Also you should run it with the same direction as client-certificate.p12 and server-certificate.p12 in Prototip1\target\classes.
- You should write server's public ip and port to connect.
- There is no register screen so you have to get your password and username from server side.
- The default port is 8333.

# Otherwise
- You are running server and client locally I suppose
- Just running Prototip1-1.0-SNAPSHOT.jar in the normal direction is ok.

### Server Side
- The application message history relies on txt file, you can find all the message history on chatlog.txt.
- Also The application holds all of the users name and hashed passwords on kullanicilar.txt.
- Only on the server management add users and delete users on server side.
- In order to run server you can compile with maven the SSLServer.java or directly open it on your IDE.
- Current pom.xml is using .Main(client side) so dont forget to change it to .SSLServer if you want to compile.


## Warning
- This app is experimental, dont rely on confidently
- The tls handshaking part has problems when you try to connect from other networks i couldnt solve it yet but i suspect that ngroks is not supporting my tls structure so you can try the app without tls handhsaking it should work fine
- If you have any problem,question or advices feel free to write a issue.

  


