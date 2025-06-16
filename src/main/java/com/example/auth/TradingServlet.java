package com.example.auth;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import java.io.*;
import java.sql.*;
import org.json.JSONObject;

@WebServlet("/trade")
public class TradingServlet extends HttpServlet {
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        try {
            StringBuilder sb = new StringBuilder();
            BufferedReader reader = request.getReader();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            JSONObject json = new JSONObject(sb.toString());

            String email = json.getString("email");
            String cryptoId = json.getString("cryptoId");
            String cryptoName = json.getString("cryptoName");
            String type = json.getString("type");
            double amount = json.getDouble("amount"); // USD if buy, crypto if sell
            double priceUsd = json.getDouble("priceUsd");

            double cryptoAmount, usdAmount;
            if ("buy".equalsIgnoreCase(type)) {
                usdAmount = amount;
                cryptoAmount = usdAmount / priceUsd;
            } else if ("sell".equalsIgnoreCase(type)) {
                cryptoAmount = amount;
                usdAmount = cryptoAmount * priceUsd;
            } else {
                out.println("{\"status\":\"fail\", \"message\":\"Invalid trade type\"}");
                return;
            }

            try (Connection conn = DBConnection.getConnection()) {
                PreparedStatement checkUser = conn.prepareStatement("SELECT balance FROM users WHERE email = ?");
                checkUser.setString(1, email);
                ResultSet rs = checkUser.executeQuery();

                if (!rs.next()) {
                    out.println("{\"status\":\"fail\", \"message\": \"User not found\"}");
                    return;
                }

                double currentBalance = rs.getDouble("balance");

             
                if ("buy".equalsIgnoreCase(type) && currentBalance < usdAmount) {
                    out.println("{\"status\":\"fail\", \"message\":\"Insufficient USD balance. You have $" + currentBalance + "\"}");
                    return;
                }

                if ("sell".equalsIgnoreCase(type)) {
                    PreparedStatement holdingStmt = conn.prepareStatement(
                        "SELECT type, crypto_amount FROM transaction WHERE email = ? AND crypto_id = ?"
                    );
                    holdingStmt.setString(1, email);
                    holdingStmt.setString(2, cryptoId);
                    ResultSet holdRs = holdingStmt.executeQuery();

                    double totalBuy = 0, totalSell = 0;
                    while (holdRs.next()) {
                        String txnType = holdRs.getString("type");
                        double amt = holdRs.getDouble("crypto_amount");
                        if ("buy".equalsIgnoreCase(txnType)) totalBuy += amt;
                        if ("sell".equalsIgnoreCase(txnType)) totalSell += amt;
                    }

                    double currentHoldings = totalBuy - totalSell;
                    if (currentHoldings < cryptoAmount) {
                        out.println("{\"status\":\"fail\", \"message\":\"Insufficient crypto holdings. You have " + currentHoldings + "\"}");
                        return;
                    }
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

                PreparedStatement recalcStmt = conn.prepareStatement(
                    "SELECT type, amount, crypto_amount, price_usd FROM transaction WHERE email = ? AND crypto_id = ? ORDER BY timestamp ASC"
                );
                recalcStmt.setString(1, email);
                recalcStmt.setString(2, cryptoId);
                ResultSet txnRs = recalcStmt.executeQuery();

                double remainingCrypto = 0;
                double remainingCost = 0;
                double sellLeft = 0;

                while (txnRs.next()) {
                    String txnType = txnRs.getString("type");
                    double amt = txnRs.getDouble("amount");
                    double cryptoAmt = txnRs.getDouble("crypto_amount");
                    double price = txnRs.getDouble("price_usd");

                    if ("buy".equalsIgnoreCase(txnType)) {
                        double toKeep = cryptoAmt;

                        if (sellLeft > 0) {
                            if (cryptoAmt <= sellLeft) {
                                sellLeft -= cryptoAmt;
                                toKeep = 0;
                            } else {
                                toKeep = cryptoAmt - sellLeft;
                                sellLeft = 0;
                            }
                        }

                        remainingCrypto += toKeep;
                        remainingCost += toKeep * price;

                    } else if ("sell".equalsIgnoreCase(txnType)) {
                        sellLeft += cryptoAmt;
                    }
                }

               


if (remainingCrypto < 1e-7) {
    remainingCrypto = 0;
    remainingCost = 0;
}

double avgCost = (remainingCrypto > 0) ? (remainingCost / remainingCrypto) : 0;
double currentValue = remainingCrypto * priceUsd;
double unrealizedProfit = currentValue - remainingCost;


                JSONObject resp = new JSONObject();
                resp.put("status", "success");
                resp.put("updatedBalance", updatedBalance);
                resp.put("cryptoId", cryptoId);
                resp.put("amountHeld", remainingCrypto);
                resp.put("avgCost", String.format("%.2f", avgCost));
                resp.put("totalInvested", String.format("%.2f", remainingCost));
                resp.put("currentValue", String.format("%.2f", currentValue));
                resp.put("unrealizedProfit", String.format("%.2f", unrealizedProfit));

                out.println(resp.toString());
            }

        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(500);
            out.println("{\"status\":\"fail\", \"message\":\"" + e.getMessage().replace("\"", "'") + "\"}");
        }
    }
}