package com.example.auth;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import java.io.*;
import java.sql.*;
import org.json.JSONObject;


@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        try {
            String username = request.getParameter("username");
            String password = request.getParameter("password");

            if (username == null || password == null) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"status\":\"error\", \"message\":\"Username or password is missing\"}");
                return;
            }

            try (Connection conn = DBConnection.getConnection()) {
                String hashedPassword = AuthUtils.hashPassword(password);

                PreparedStatement stmt = conn.prepareStatement(
                    "SELECT email FROM users WHERE username = ? AND password = ?"
                );
                stmt.setString(1, username);
                stmt.setString(2, hashedPassword);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    String email = rs.getString("email");
                    String accessToken = AuthUtils.generateAccessToken(email);

                    JSONObject json = new JSONObject();
                    json.put("status", "success");
                    json.put("email", email);
                    json.put("accessToken", accessToken);

                    response.setStatus(HttpServletResponse.SC_OK);
                    out.print(json);
                } else {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    out.print("{\"status\":\"failed\", \"message\":\"Invalid username or password\"}");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"status\":\"error\", \"message\":\"" + e.getMessage().replace("\"", "'") + "\"}");
        } finally {
            out.close();
        }
    }
}
