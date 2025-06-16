package com.example.auth;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import java.io.*;
import java.security.MessageDigest;
import java.sql.*;
import org.json.JSONObject;
@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    private String hashPassword(String password) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashedBytes = digest.digest(password.getBytes("UTF-8"));
        StringBuilder sb = new StringBuilder();
        for (byte b : hashedBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {

        String username = request.getParameter("username");
        String password = request.getParameter("password");

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        try (Connection conn = DBConnection.getConnection()) {
            String hashedPassword = hashPassword(password);

            PreparedStatement stmt = conn.prepareStatement(
                "SELECT email FROM users WHERE username = ? AND password = ?"
            );
            stmt.setString(1, username);
            stmt.setString(2, hashedPassword);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String email = rs.getString("email");
                out.println("{\"status\":\"success\", \"email\":\"" + email + "\"}");
            } else {
                out.println("{\"status\":\"failed\"}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            out.println("{\"status\":\"error\", \"message\":\"" + e.getMessage() + "\"}");
        }
    }
}
