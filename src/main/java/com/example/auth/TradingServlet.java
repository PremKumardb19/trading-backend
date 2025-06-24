package com.example.auth;

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
                } else { 
                    cryptoAmount = amount;
                    usdAmount = cryptoAmount * priceUsd;
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
                        else if ("sell".equalsIgnoreCase(txnType)) totalSell += amt;
                    }

                    double currentHoldings = totalBuy - totalSell;
                    if (currentHoldings < cryptoAmount) {
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
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
