package com.example.auth;

import java.io.*;
import java.security.MessageDigest;
import java.sql.*;
import java.util.Date;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import org.json.JSONObject;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;

@WebServlet("/register")
public class RegisterServlet extends HttpServlet {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/trading";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "";
    private static final String SECRET_KEY = "pk1908seckeyret007";

   
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
        Algorithm algorithm = Algorithm.HMAC256(SECRET_KEY);
        return JWT.create()
                .withSubject(username)
                .withClaim("email", email)
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + 86400000)) 
                .sign(algorithm);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

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
            String username = json.getString("username");
            String email = json.getString("email");
            String password = json.getString("password");

            
            String hashedPassword = hashPassword(password);

               
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
                String sql = "INSERT INTO users (username, email, password) VALUES (?, ?, ?)";
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setString(1, username);
                stmt.setString(2, email);
                stmt.setString(3, hashedPassword);

                int rows = stmt.executeUpdate();
                if (rows > 0) {
                    String token = generateToken(username, email);

                    JSONObject resJson = new JSONObject();
                    resJson.put("status", "success");
                    resJson.put("message", "User registered successfully");
                    resJson.put("token", token);
                    resJson.put("email", email);

                    response.setStatus(HttpServletResponse.SC_OK);
                    out.print(resJson.toString());
                } else {
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    out.print("{\"status\":\"failed\",\"message\":\"Failed to register user\"}");
                }
            }

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"status\":\"error\",\"error\":\"" + e.getMessage() + "\"}");
        } finally {
            out.close();
        }
    }
}
