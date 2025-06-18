package com.example.auth;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import java.io.*;
import java.sql.*;
import org.json.JSONObject;

@WebServlet("/holdings")
public class HoldingServlet extends HttpServlet {

    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.setContentType("application/json");
        PrintWriter out = res.getWriter();

        String email = req.getParameter("email");
        String cryptoId = req.getParameter("cryptoId");
        String priceUsdStr = req.getParameter("priceUsd");

        if (email == null || cryptoId == null || priceUsdStr == null) {
            res.setStatus(400);
            out.print("{\"error\":\"Missing parameters: email, cryptoId, priceUsd required\"}");
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            double currentPrice = Double.parseDouble(priceUsdStr);

            double totalCryptoBuy = 0;
            double totalCryptoSell = 0;
            double totalUsdBuy = 0;
            double totalUsdSell = 0;

            String sql = "SELECT type, amount, crypto_amount FROM transaction WHERE email = ? AND crypto_id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, email);
            ps.setString(2, cryptoId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String type = rs.getString("type");
                double cryptoAmount = rs.getDouble("crypto_amount");
                double usdAmount = rs.getDouble("amount");

                if ("buy".equalsIgnoreCase(type)) {
                    totalCryptoBuy += cryptoAmount;
                    totalUsdBuy += usdAmount;
                } else if ("sell".equalsIgnoreCase(type)) {
                    totalCryptoSell += cryptoAmount;
                    totalUsdSell += usdAmount;
                }
            }

            double amountHeld = totalCryptoBuy - totalCryptoSell;
            if (amountHeld < 1e-7) amountHeld = 0;

            double avgBuyPrice = (totalCryptoBuy > 0) ? totalUsdBuy / totalCryptoBuy : 0;
            double currentValue = amountHeld * currentPrice;

           
            double totalProfitOrLoss = currentValue + totalUsdSell - totalUsdBuy;
            if(amountHeld==0)totalProfitOrLoss=0;
            JSONObject json = new JSONObject();
            json.put("email", email);
            json.put("cryptoId", cryptoId);
            json.put("amountHeld", amountHeld);
            json.put("totalInvested", totalUsdBuy);
            json.put("avgBuyPrice", avgBuyPrice);
            json.put("currentValue", currentValue);
            json.put("profitOrLoss", totalProfitOrLoss);

            out.print(json.toString());

        } catch (Exception e) {
            e.printStackTrace();
            res.setStatus(500);
            out.print("{\"error\":\"" + e.getMessage().replace("\"", "'") + "\"}");
        }
    }
}