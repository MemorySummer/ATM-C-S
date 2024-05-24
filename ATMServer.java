import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.util.logging.*;
import java.sql.*;

public class ATMServer {
    private static final int PORT = 2525;
    private static final Logger LOGGER = Logger.getLogger(ATMServer.class.getName());

    static final String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";
    static final String DB_URL = "jdbc:mysql://localhost:3306/atm";

    static final String USER = "root";
    static final String PASS = "520114";

    public static void main(String[] args) throws ClassNotFoundException, SQLException {

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            LOGGER.info("Server started. Listening on port " + PORT + "...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                LOGGER.info("Client connected: " + clientSocket);

                Thread clientHandler = new Thread(new ClientHandler(clientSocket));
                clientHandler.start();
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Server exception: " + e.getMessage(), e);
        }
    }

    private static class ClientHandler implements Runnable {
        private final Socket clientSocket;
        private final Logger logger;

        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
            this.logger = Logger.getLogger("Client " + clientSocket.getInetAddress());
        }

        public void run() {

            Connection conn = null;
            String Usernow = null;

            try {
                conn = DriverManager.getConnection(DB_URL,USER,PASS);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            Statement stmt = null;
            try {
                stmt = conn.createStatement();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }

            ResultSet rs = null;
            try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                 PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {
                Class.forName(JDBC_DRIVER);

                String inputLine;

                while ((inputLine = in.readLine()) != null) {

                    logger.info("Received from client: " + inputLine);
                    String[] tokens = inputLine.split(" ");

                    if (tokens[0].equals("HELO")) {

                        String query = "SELECT * FROM atm.client WHERE UserID = '" + tokens[1] + "'";
                        rs = stmt.executeQuery(query);

                        if (rs.next()) {

                            out.println("500 AUTH REQUIRED!");
                            Usernow =tokens[1];

                        } else {

                            out.println("401 ERROR!");

                        }


                    } else if (tokens[0].equals("PASS")) {

                        String query = "SELECT * FROM atm.client WHERE Password = '" + tokens[1] + "'";
                        rs = stmt.executeQuery(query);

                        if(rs.next()){

                            out.println("525 OK!");
                            LocalDateTime currentTime = LocalDateTime.now();
                            String data = currentTime+":用户"+Usernow+"账号验证成功！\n";
                            File file = new File("message.txt");
                            FileWriter fileWritter = new FileWriter(file.getName(),true);
                            fileWritter.write(data);
                            fileWritter.close();

                        } else {

                            out.println("401 ERROR!");

                        }

                    } else if (tokens[0].equals("BALA")) {

                        String query = "SELECT Balance FROM atm.client WHERE UserID = '" + Usernow + "'";
                        rs = stmt.executeQuery(query);
                        rs.next();
                        double balance = rs.getDouble("Balance");
                        out.println("AMNT: "+balance);
                        LocalDateTime currentTime = LocalDateTime.now();
                        String data = currentTime+":用户"+Usernow+"查询了余额\n";
                        File file = new File("message.txt");
                        FileWriter fileWritter = new FileWriter(file.getName(),true);
                        fileWritter.write(data);
                        fileWritter.close();

                    } else if (tokens[0].equals("WDRA")) {

                        String query = "SELECT Balance FROM atm.client WHERE UserID = '" + Usernow + "'";
                        rs = stmt.executeQuery(query);
                        rs.next();
                        double balance = rs.getDouble("Balance");
                        logger.info("Withdrawal request: " + tokens[1]);
                        double withdrawamount = Double.parseDouble(tokens[1]);
                        String withdrawnumber = String.valueOf(withdrawamount);

                        if(withdrawamount<=balance){

                            out.println("525 OK!");
                            int rs1 = stmt.executeUpdate("UPDATE client SET Balance = Balance - "+ withdrawnumber + "WHERE UserID = '" + Usernow + "'");
                            LocalDateTime currentTime = LocalDateTime.now();
                            String data = currentTime+":用户"+Usernow+"取出了"+withdrawamount+"元\n";
                            File file = new File("message.txt");
                            FileWriter fileWritter = new FileWriter(file.getName(),true);
                            fileWritter.write(data);
                            fileWritter.close();
                        } else {

                            out.println("401 ERROR!");

                        }
                    } else if (tokens[0].equals("BYE")) {

                        out.println("BYE");
                        LocalDateTime currentTime = LocalDateTime.now();
                        String data = currentTime+":用户"+Usernow+"登出成功\n";
                        File file = new File("message.txt");
                        FileWriter fileWritter = new FileWriter(file.getName(),true);
                        fileWritter.write(data);
                        fileWritter.close();
                        Usernow = null;
                        break;

                    } else {

                        out.println("401  ERROR!");

                    }
                }

                clientSocket.close();
            } catch (IOException e) {
                logger.log(Level.SEVERE, "ClientHandler exception: " + e.getMessage(), e);
            } catch (ClassNotFoundException | SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
