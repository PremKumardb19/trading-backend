package com.example.auth;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import java.io.*;
import java.security.MessageDigest;
import java.sql.*;
import java.util.Date;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    private static final String SECRET_KEY = "pk1908seckeyret007";
    private static final long EXPIRATION_TIME = 86400000; 

    private String hashPassword(String password) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashedBytes = digest.digest(password.getBytes("UTF-8"));
        StringBuilder sb = new StringBuilder();
        for (byte b : hashedBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private String generateToken(String username, String email) {
        return JWT.create()
            .withSubject(username)
            .withClaim("email", email)
            .withIssuedAt(new Date())
            .withExpiresAt(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
            .sign(Algorithm.HMAC256(SECRET_KEY));
    }

    @Override
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
                String token = generateToken(username, email);

                out.println("{");
                out.println("\"status\":\"success\",");
                out.println("\"email\":\"" + email + "\",");
                out.println("\"token\":\"" + token + "\"");
                out.println("}");
            } else {
                out.println("{\"status\":\"failed\"}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            out.println("{\"status\":\"error\", \"message\":\"" + e.getMessage() + "\"}");
        }
    }
}
