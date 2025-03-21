package org.example;
import javax.swing.*;


public class PasswordGUI extends JFrame {
    boolean isRegistered;
    //TODO make action listener to button and chechk whether the user is registered or not if not return false and if false than wr,ite out
    //TODO if the password and username on database than let the user open the second window(messaging one)
    //TODO Return a boolean value that will ensure the user registered for the second problem
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
