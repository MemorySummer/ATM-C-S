import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.logging.*;

public class ATMClientGUI extends JFrame {
    private static final String SERVER_ADDRESS = "10.234.107.83";
    private static final int SERVER_PORT = 2525;
    private static final Logger LOGGER = Logger.getLogger(ATMClientGUI.class.getName());

    private JTextField userIdField;
    private JTextField passwordField;
    private JButton loginButton;
    private JButton balanceButton;
    private JButton withdrawButton;
    private JButton logoutButton;
    private JTextArea logArea;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    public ATMClientGUI() {
        setTitle("ATM Client");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        userIdField = new JTextField(10);
        passwordField = new JPasswordField(10);
        loginButton = new JButton("Login");
        balanceButton = new JButton("Check Balance");
        withdrawButton = new JButton("Withdraw");
        logoutButton = new JButton("Logout");
        logArea = new JTextArea();
        logArea.setEditable(false);

        JPanel loginPanel = new JPanel();
        loginPanel.add(new JLabel("User ID:"));
        loginPanel.add(userIdField);
        loginPanel.add(new JLabel("Password:"));
        loginPanel.add(passwordField);
        loginPanel.add(loginButton);
        loginPanel.add(balanceButton);
        loginPanel.add(withdrawButton);
        loginPanel.add(logoutButton);

        add(loginPanel, BorderLayout.NORTH);
        add(new JScrollPane(logArea), BorderLayout.CENTER);

        loginButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                login();
            }
        });

        balanceButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                checkBalance();
            }
        });

        withdrawButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                withdraw();
            }
        });

        logoutButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                logout();
            }
        });

        setVisible(true);
    }

    private void login() {
        String userId = userIdField.getText();
        String password = passwordField.getText();

        try {
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            out.println("HELO " + userId);
            logArea.append("Sent to server: HELO " + userId + "\n");

            String response = in.readLine();
            logArea.append("Received from server: " + response + "\n");

            if (response.startsWith("500 AUTH REQUIRE")) {
                out.println("PASS " + password);
                logArea.append("Sent to server: PASS " + password + "\n");

                response = in.readLine();
                logArea.append("Received from server: " + response + "\n");

                if (response.startsWith("525 OK!")) {
                    logArea.append("Login successful.\n");
                } else {
                    logArea.append("Login failed.The password is incorrect.\n");
                }
            } else {
                logArea.append("Login failed. The user doesn't exist.\n");
            }
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Error during login: " + ex.getMessage(), ex);
            logArea.append("Error during login: " + ex.getMessage() + "\n");
        }
    }

    private void checkBalance() {
        try {
            out.println("BALA");
            logArea.append("Sent to server: BALA\n");

            String response = in.readLine();
            logArea.append("Received from server: " + response + "\n");

            if (response.startsWith("AMNT:")) {
                String balance = response.substring(5);
                logArea.append("Your balance: " + balance + "\n");
            } else {
                logArea.append("Failed to retrieve balance.\n");
            }
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Error checking balance: " + ex.getMessage(), ex);
            logArea.append("Error checking balance: " + ex.getMessage() + "\n");
        }
    }

    private void withdraw() {
        String amount = JOptionPane.showInputDialog(this, "Enter withdrawal amount:");

        try {
            out.println("WDRA " + amount);
            logArea.append("Sent to server: WDRA " + amount + "\n");

            String response = in.readLine();
            logArea.append("Received from server: " + response + "\n");

            if (response.startsWith("525 OK!")) {
                logArea.append("Withdrawal successful.\n");
            } else {
                logArea.append("Withdrawal failed.\n");
            }
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Error withdrawing: " + ex.getMessage(), ex);
            logArea.append("Error withdrawing: " + ex.getMessage() + "\n");
        }
    }

    private void logout() {
        try {
            out.println("BYE");
            logArea.append("Sent to server: BYE\n");

            String response = in.readLine();
            logArea.append("Received from server: " + response + "\n");

            if (response.equals("BYE")) {
                logArea.append("Logged out.\n");
                socket.close();
            } else {
                logArea.append("Failed to logout.\n");
            }
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Error logging out: " + ex.getMessage(), ex);
            logArea.append("Error logging out: " + ex.getMessage() + "\n");
        }
    }

    public static void main(String[] args) {

        try {
            FileHandler fileHandler = new FileHandler("client.log");
            LOGGER.addHandler(fileHandler);
            SimpleFormatter formatter = new SimpleFormatter();
            fileHandler.setFormatter(formatter);
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Error setting up logging: " + ex.getMessage(), ex);
        }

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new ATMClientGUI();
            }
        });
    }
}
