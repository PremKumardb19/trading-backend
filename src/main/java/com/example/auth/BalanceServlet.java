package com.example.auth;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/balance")
public class BalanceServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.setContentType("application/json");
        PrintWriter out = res.getWriter();

        String email = (String) req.getAttribute("tokenEmail");

        if (email == null) {
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print("{\"error\":\"Unauthorized: email not found in request context\"}");
            return;
        }

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

        } catch (Exception e) {
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\": \"" + e.getMessage().replace("\"", "'") + "\"}");
        }
    }
}
