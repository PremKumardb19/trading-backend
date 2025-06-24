package trading;

import db.DBConnection;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.*;
import java.sql.*;
import org.json.JSONObject;

@WebServlet("/wallet")
public class WalletServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.setContentType("application/json");
        PrintWriter out = res.getWriter();

        String email = (String) req.getAttribute("tokenEmail");
        String action = req.getParameter("action");

        if (email == null) {
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print("{\"error\":\"Unauthorized access\"}");
            return;
        }

        if (action == null) {
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"error\":\"Missing 'action' parameter\"}");
            return;
        }

        try {
            switch (action.toLowerCase()) {
                case "balance":
                    handleBalance(email, res, out);
                    break;
                case "holdings":
                    handleHoldings(email, req, res, out);
                    break;
                default:
                    res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.print("{\"error\":\"Invalid action for GET. Use 'balance' or 'holdings'\"}");
            }
        } catch (Exception e) {
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\":\"" + e.getMessage().replace("\"", "'") + "\"}");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.setContentType("application/json");
        PrintWriter out = res.getWriter();

        String email = (String) req.getAttribute("tokenEmail");
        String action = req.getParameter("action");

        if (email == null) {
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print("{\"error\":\"Unauthorized access\"}");
            return;
        }

        if (action == null) {
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"error\":\"Missing 'action' parameter\"}");
            return;
        }

        try {
            switch (action.toLowerCase()) {
                case "funding":
                    handleFunding(email, req, res, out);
                    break;
                default:
                    res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.print("{\"error\":\"Invalid action for POST. Use 'action=funding'\"}");
            }
        } catch (Exception e) {
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\":\"" + e.getMessage().replace("\"", "'") + "\"}");
        }
    }

    private void handleBalance(String email, HttpServletResponse res, PrintWriter out) throws Exception {
        try (Connection conn = DBConnection.getConnection()) {
            PreparedStatement ps = conn.prepareStatement("SELECT balance FROM users WHERE email = ?");
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                double balance = rs.getDouble("balance");
                res.setStatus(HttpServletResponse.SC_OK);
                out.print("{\"balance\": " + balance + "}");
            } else {
                res.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.print("{\"error\":\"User not found\"}");
            }
        }
    }

    private void handleHoldings(String email, HttpServletRequest req, HttpServletResponse res, PrintWriter out) throws Exception {
        String cryptoId = req.getParameter("cryptoId");
        String priceUsdStr = req.getParameter("priceUsd");

        if (cryptoId == null || priceUsdStr == null) {
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"error\":\"Missing required parameters: cryptoId and priceUsd\"}");
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            double currentPrice = Double.parseDouble(priceUsdStr);
            double totalCryptoBuy = 0, totalCryptoSell = 0;
            double totalUsdBuy = 0, totalUsdSell = 0;

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
            double totalProfitOrLoss = (amountHeld > 0) ? currentValue + totalUsdSell - totalUsdBuy : 0;

            JSONObject json = new JSONObject();
            json.put("email", email);
            json.put("cryptoId", cryptoId);
            json.put("amountHeld", amountHeld);
            json.put("totalInvested", totalUsdBuy);
            json.put("avgBuyPrice", avgBuyPrice);
            json.put("currentValue", currentValue);
            json.put("profitOrLoss", totalProfitOrLoss);

            res.setStatus(HttpServletResponse.SC_OK);
            out.print(json);
        } catch (NumberFormatException e) {
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"error\":\"Invalid price format\"}");
        }
    }

    private void handleFunding(String email, HttpServletRequest req, HttpServletResponse res, PrintWriter out) throws Exception {
        String amountParam = req.getParameter("amount");
        if (amountParam == null || amountParam.isEmpty()) {
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"error\":\"Missing amount parameter\"}");
            return;
        }

        try {
            int amount = Integer.parseInt(amountParam);
            try (Connection conn = DBConnection.getConnection()) {
                PreparedStatement stmt = conn.prepareStatement(
                    "UPDATE users SET balance = balance + ? WHERE email = ?"
                );
                stmt.setInt(1, amount);
                stmt.setString(2, email);

                int rows = stmt.executeUpdate();
                if (rows > 0) {
                    res.setStatus(HttpServletResponse.SC_OK);
                    out.print("{\"status\":\"success\", \"message\":\"Funds added\"}");
                } else {
                    res.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    out.print("{\"status\":\"failed\", \"message\":\"User not found\"}");
                }
            }
        } catch (NumberFormatException e) {
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"error\":\"Invalid amount format\"}");
        }
    }
}
