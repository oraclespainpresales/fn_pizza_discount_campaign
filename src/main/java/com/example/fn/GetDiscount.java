package com.example.fn;

//import com.cedarsoftware.util.io.JsonWriter;
import java.sql.*;
import java.io.PrintWriter;
import java.io.StringWriter;

public class GetDiscount {

    public static class Input {
        public String demozone;
        public String paymentMethod;
        public String pizzaPrice;

        public String toString() {
            StringBuilder stb = new StringBuilder("{");
            stb.append("'demozone':'").append(demozone).append("'");
            stb.append("'paymentMethod':'").append(paymentMethod).append("'");
            stb.append("'pizzaPrice':'").append(pizzaPrice).append("'");
            stb.append("}");
            return stb.toString();
        }
    }

    public String handleRequest(Input pizzaData) {
        String exitValues    = "SALIDA::";
        ResultSet resultSet  = null;
        Connection con       = null;
        float discount       = 0;

        try {
            String paymentMethod = pizzaData.paymentMethod.toUpperCase();
            String demozone      = pizzaData.demozone.toUpperCase();
            String pizzaPrice    = pizzaData.pizzaPrice;

            //cast string input into a float
            System.err.println("inside Discount Function gigis fn function!!! ");
            float totalPaidValue  = Float.parseFloat(pizzaPrice);

            String dbUser         = System.getenv().get("DB_USER");
            String dbPassword     = System.getenv().get("DB_PASSWORD");
            String dbUrl          = System.getenv().get("DB_URL") + System.getenv().get("DB_SERVICE_NAME");
            String clientCredPath = System.getenv().get("CLIENT_CREDENTIALS");
            String keyStorePasswd = System.getenv().get("KEYSTORE_PASSWORD");
            String truStorePasswd = System.getenv().get("TRUSTSTORE_PASSWORD");
            
            /*********** FOR TESTING ONLY *************************************
            System.err.println("ENV::" + dbUser);
            System.err.println("ENV::" + dbPassword);
            System.err.println("ENV::" + dbUrl);
            System.err.println("ENV::" + clientCredPath);            
            System.err.println("ENV::" + keyStorePasswd);
            System.err.println("ENV::" + truStorePasswd);       
            */

            System.setProperty("oracle.jdbc.driver.OracleDriver", "true");
            System.setProperty("oracle.net.ssl_version", "1.2");
            System.setProperty("javax.net.ssl.keyStore", "${clientCredPath}/keystore.jks");
            System.setProperty("javax.net.ssl.keyStorePassword", keyStorePasswd);
            System.setProperty("javax.net.ssl.trustStore", "${clientCredPath}/truststore.jks");
            System.setProperty("javax.net.ssl.trustStorePassword", truStorePasswd);
            System.setProperty("oracle.net.tns_admin", clientCredPath);

            try {
                DriverManager.registerDriver(new oracle.jdbc.OracleDriver());
                //System.err.println("QUERY:: Driver Registration [" + dbUrl +" "+ dbUser+" "+dbPassword + "]");
                con = DriverManager.getConnection(dbUrl,dbUser,dbPassword);
                if (con != null) {
                    System.err.println("Connected to Oracle ATP DB successfully");                
                    //System.err.println("QUERY:: Driver getConnection");

                    StringBuilder stb = new StringBuilder("SELECT NVL (");
                    stb.append("(SELECT SUM(DISCOUNT) FROM CAMPAIGN WHERE ");
                    stb.append("DEMOZONE LIKE ? ");
                    stb.append("AND PAYMENTMETHOD LIKE ? ");
                    stb.append("AND CURRENT_DATE BETWEEN DATE_BGN AND DATE_END+1 ");
                    stb.append("AND MIN_AMOUNT <= ?)");
                    stb.append(",0) as DISCOUNT FROM DUAL");
                    PreparedStatement pstmt = con.prepareStatement(stb.toString());

                    pstmt.setString(1,demozone);
                    pstmt.setString(2,paymentMethod);
                    pstmt.setFloat(3,Float.parseFloat(pizzaPrice));
                    
                    /*********** FOR TESTING ONLY *************************************
                    System.err.println("QUERY::    " + stb.toString());
                    System.err.println("QUERY:: 1. " + demozone);
                    System.err.println("QUERY:: 2. " + paymentMethod);
                    System.err.println("QUERY:: 3. " + pizzaPrice);
                    */
                    
                    System.err.println("[" + pizzaData.toString() + "] - Pizza Price before discount: " + totalPaidValue + "$");
                    resultSet = pstmt.executeQuery();
                    if (resultSet.next()){                                                
                        discount = Float.parseFloat(resultSet.getString("DISCOUNT"))/100;                        
                        if (discount > 0){
                            //apply calculation to float eg: discount = 10%
                            totalPaidValue -=  (totalPaidValue*discount);
                            System.err.println("[" + pizzaData.toString() + "] - discount: " + resultSet.getString("DISCOUNT") + "%");
                        }
                        else
                            System.err.println ("[" + pizzaData.toString() + "] - No Discount campaign for this payment! [0%]");
                    }
                    else {
                        System.err.println ("[" + pizzaData.toString() + "] - No Discount campaign for this payment!");
                    }
                    System.err.println("[" + pizzaData.toString() + "] - total Pizza Price after discount: " + totalPaidValue + "$");
                    exitValues = Float.toString(totalPaidValue);
                }
                else {
                    System.err.println("[" + pizzaData.toString() + "] - Error: DB Connection Problem"); 
                    throw new NullPointerException("DB Connection Problem : NULL!");
                }
            }     
            catch (Exception ex) {
                StringWriter errors = new StringWriter();
                ex.printStackTrace(new PrintWriter(errors));                 
                exitValues = pizzaData.toString() + " - Error: " + ex.toString() + "\n" + ex.getMessage() + errors.toString();                
            }  
            finally {
                con.close();
            }
        }
        catch (Exception ex){
            StringWriter errors = new StringWriter();
            ex.printStackTrace(new PrintWriter(errors));                 
            exitValues = pizzaData.toString() + " - Error: " + ex.toString() + "\n" + ex.getMessage() + errors.toString();;
        }
        finally{
            return exitValues;
        }
    }
}