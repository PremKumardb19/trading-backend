package com.example.auth;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import java.io.*;
import java.sql.*;
import org.json.JSONObject;

@WebServlet("/register")
public class RegisterServlet extends HttpServlet {

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

            String hashedPassword = AuthUtils.hashPassword(password);

            try (Connection conn = DBConnection.getConnection()) {
                PreparedStatement checkStmt = conn.prepareStatement("SELECT email FROM users WHERE email = ?");
                checkStmt.setString(1, email);
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next()) {
                    response.setStatus(HttpServletResponse.SC_CONFLICT); 
                    out.print(new JSONObject()
                        .put("status", "failed")
                        .put("message", "User with this email already exists")
                        .toString());
                    return;
                }

                String sql = "INSERT INTO users (username, email, password) VALUES (?, ?, ?)";
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setString(1, username);
                stmt.setString(2, email);
                stmt.setString(3, hashedPassword);

                int rows = stmt.executeUpdate();
                if (rows > 0) {
                    String accessToken = AuthUtils.generateAccessToken(email);

                    JSONObject resJson = new JSONObject();
                    resJson.put("status", "success");
                    resJson.put("message", "User registered successfully");
                    resJson.put("email", email);
                    resJson.put("accessToken", accessToken);

                    response.setStatus(HttpServletResponse.SC_OK); 
                    out.print(resJson);
                } else {
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR); 
                    out.print(new JSONObject()
                        .put("status", "failed")
                        .put("message", "Failed to register user")
                        .toString());
                }
            }

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST); 
            out.print(new JSONObject()
                .put("status", "error")
                .put("message", e.getMessage())
                .toString());
        } finally {
            out.close();
        }
    }
}
