package org.example;
import java.awt.*;
import javax.print.attribute.UnmodifiableSetException;
import javax.swing.*;


public class MessageBoxGUI extends JFrame{
    public static void StartGUI() {

        //TODO UserName 1/2/3/4 will show of w.r.t the combox and it will change
        //TODO Messages going to upload from the server with username and the message
        //TODO also a new label will be added with a loop when it gets any new messages that will help that a new messahe to pop up
        //TODO repaint all the GraphicalCompenent with a regular time like 30 or 60fps
        //TODO Always Check whether the isRegistered or isLogin is True for security purposes

        JFrame frame = new JFrame("The Messaging Part");
        JPanel panel = new JPanel();
        panel.setLayout(null);
        panel.setForeground(Color.black);
        panel.setBackground(Color.gray);
        panel.setBounds(80,80,300,300);

        JComboBox comboBox = new JComboBox();
        JButton SentButton = new JButton("Sent");
        JLabel UserLabel = new JLabel("User1/2/3/4");
        JLabel MessageBoxLabel = new JLabel("AAAA");
        JTextField TextField = new JTextField();

        MessageBoxLabel.setBounds(10, 10, 200, 10);
        panel.add(MessageBoxLabel);
        comboBox.setEditable(false);
        comboBox.addItem("User1");
        comboBox.addItem("User2");
        comboBox.addItem("User3");
        comboBox.addItem("User4");


        comboBox.setBounds(80, 10, 200, 30);
        SentButton.setBounds(80, 430, 100, 30);
        TextField.setBounds(80, 395, 200, 20);

        UserLabel.setBounds(80, 40, 100, 30);
        frame.add(comboBox);
        frame.add(SentButton);
        frame.add(panel);
        frame.add(UserLabel);
        frame.add(TextField);

        frame.setSize(500, 500);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.setLayout(null);
        frame.setVisible(true);

    }
}
