package trading;

import db.DBConnection;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import java.io.*;
import java.sql.*;
import org.json.JSONObject;

@WebServlet("/trade")
public class TradingServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        String email = (String) request.getAttribute("tokenEmail");

        if (email == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.println("{\"status\":\"fail\", \"message\":\"Unauthorized: missing user info\"}");
            return;
        }

        try {
            StringBuilder sb = new StringBuilder();
            BufferedReader reader = request.getReader();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            JSONObject json = new JSONObject(sb.toString());

            String cryptoId = json.optString("cryptoId", null);
            String cryptoName = json.optString("cryptoName", null);
            String type = json.optString("type", null);
            double amount = json.optDouble("amount", -1);
            double priceUsd = json.optDouble("priceUsd", -1);

            if (cryptoId == null || cryptoName == null || type == null || amount <= 0 || priceUsd <= 0) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.println("{\"status\":\"fail\", \"message\":\"Missing or invalid parameters\"}");
                return;
            }

            if (!("buy".equalsIgnoreCase(type) || "sell".equalsIgnoreCase(type))) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.println("{\"status\":\"fail\", \"message\":\"Invalid trade type\"}");
                return;
            }

            try (Connection conn = DBConnection.getConnection()) {
                conn.setAutoCommit(false);

               
                PreparedStatement checkUser = conn.prepareStatement("SELECT balance FROM users WHERE email = ?");
                checkUser.setString(1, email);
                ResultSet rs = checkUser.executeQuery();

                if (!rs.next()) {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    out.println("{\"status\":\"fail\", \"message\": \"User not found\"}");
                    return;
                }

                double currentBalance = rs.getDouble("balance");

                double cryptoAmount, usdAmount;

                if ("buy".equalsIgnoreCase(type)) {
                    usdAmount = amount;
                    cryptoAmount = usdAmount / priceUsd;

                    if (currentBalance < usdAmount) {
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        out.println("{\"status\":\"fail\", \"message\":\"Insufficient USD balance. You have $" + currentBalance + "\"}");
                        return;
                    }

                   
                    PreparedStatement checkCap = conn.prepareStatement(
                        "SELECT sold_crypto_amount, market_cap FROM crypto_inventory WHERE crypto_id = ? FOR UPDATE"
                    );
                    checkCap.setString(1, cryptoId);
                    ResultSet capRs = checkCap.executeQuery();

                    double alreadySoldCrypto = 0;
                    double marketCap = 0;
                    if (capRs.next()) {
                        alreadySoldCrypto = capRs.getDouble("sold_crypto_amount");
                        marketCap = capRs.getDouble("market_cap");
                    } else {
                        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        out.println("{\"status\":\"fail\", \"message\":\"Crypto not initialized in inventory\"}");
                        conn.rollback();
                        return;
                    }

                    if (alreadySoldCrypto + cryptoAmount > marketCap) {
                        double remaining = marketCap - alreadySoldCrypto;
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        out.println("{\"status\":\"fail\", \"message\":\"Market cap exceeded. Only " + remaining + " units available.\"}");
                        conn.rollback();
                        return;
                    }

                    PreparedStatement updateCap = conn.prepareStatement(
                        "UPDATE crypto_inventory SET sold_crypto_amount = sold_crypto_amount + ? WHERE crypto_id = ?"
                    );
                    updateCap.setDouble(1, cryptoAmount);
                    updateCap.setString(2, cryptoId);
                    updateCap.executeUpdate();

                } else {
                   
                    cryptoAmount = amount;
                    usdAmount = cryptoAmount * priceUsd;

                    
                    PreparedStatement holdingStmt = conn.prepareStatement(
                        "SELECT type, crypto_amount FROM transaction WHERE email = ? AND crypto_id = ? FOR UPDATE"
                    );
                    holdingStmt.setString(1, email);
                    holdingStmt.setString(2, cryptoId);
                    ResultSet holdRs = holdingStmt.executeQuery();

                    double totalBuy = 0, totalSell = 0;
                    while (holdRs.next()) {
                        String txnType = holdRs.getString("type");
                        double amt = holdRs.getDouble("crypto_amount");
                        if ("buy".equalsIgnoreCase(txnType)) totalBuy += amt;
                        else if ("sell".equalsIgnoreCase(txnType)) totalSell += amt;
                    }

                    double currentHoldings = totalBuy - totalSell;

                    if (currentHoldings < cryptoAmount) {
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        out.println("{\"status\":\"fail\", \"message\":\"Insufficient crypto holdings. You have " + currentHoldings + "\"}");
                        conn.rollback();
                        return;
                    }
                    
                        PreparedStatement checkCap = conn.prepareStatement(
                            "SELECT sold_crypto_amount FROM crypto_inventory WHERE crypto_id = ? FOR UPDATE"
                        );
                        checkCap.setString(1, cryptoId);
                        ResultSet capRs = checkCap.executeQuery();

                        double alreadySoldCrypto = 0;
                        if (capRs.next()) {
                            alreadySoldCrypto = capRs.getDouble("sold_crypto_amount");
                        } else {
                            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                            out.println("{\"status\":\"fail\", \"message\":\"Crypto not initialized in inventory\"}");
                            conn.rollback();
                            return;
                        }

                        if (cryptoAmount > alreadySoldCrypto) {
                            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                            out.println("{\"status\":\"fail\", \"message\":\"Internal error: Trying to sell more than sold\"}");
                            conn.rollback();
                            return;
                        }


                        PreparedStatement updateCap = conn.prepareStatement(
                            "UPDATE crypto_inventory SET sold_crypto_amount = sold_crypto_amount - ? WHERE crypto_id = ?"
                        );
                        updateCap.setDouble(1, cryptoAmount);
                        updateCap.setString(2, cryptoId);
                        updateCap.executeUpdate();

                }

            
                PreparedStatement insertTxn = conn.prepareStatement(
                    "INSERT INTO transaction (email, crypto_id, crypto_name, amount, crypto_amount, price_usd, type, timestamp) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, NOW())"
                );
                insertTxn.setString(1, email);
                insertTxn.setString(2, cryptoId);
                insertTxn.setString(3, cryptoName);
                insertTxn.setDouble(4, usdAmount);
                insertTxn.setDouble(5, cryptoAmount);
                insertTxn.setDouble(6, priceUsd);
                insertTxn.setString(7, type);
                insertTxn.executeUpdate();

             
                double updatedBalance = "buy".equalsIgnoreCase(type)
                    ? currentBalance - usdAmount
                    : currentBalance + usdAmount;

                PreparedStatement updateUser = conn.prepareStatement("UPDATE users SET balance = ? WHERE email = ?");
                updateUser.setDouble(1, updatedBalance);
                updateUser.setString(2, email);
                updateUser.executeUpdate();

                conn.commit();

                JSONObject resp = new JSONObject();
                resp.put("status", "success");
                resp.put("updatedBalance", updatedBalance);
                resp.put("cryptoId", cryptoId);
                resp.put("cryptoAmount", cryptoAmount);
                resp.put("usdAmount", usdAmount);

                response.setStatus(HttpServletResponse.SC_OK);
                out.println(resp);
            }

        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.println("{\"status\":\"fail\", \"message\":\"" + e.getMessage().replace("\"", "'") + "\"}");
        }
    }
}
