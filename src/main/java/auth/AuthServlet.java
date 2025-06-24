package auth;

import db.DBConnection;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.*;
import java.sql.*;
import org.json.JSONObject;

@WebServlet("/auth")
public class AuthServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        String action = request.getParameter("action");

        if (action == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"status\":\"error\", \"message\":\"Missing 'action' parameter (login or register)\"}");
            return;
        }

        try {
            if (action.equalsIgnoreCase("login")) {
                handleLogin(request, response, out);
            } else if (action.equalsIgnoreCase("register")) {
                handleRegister(request, response, out);
            } else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"status\":\"error\", \"message\":\"Invalid action. Use 'login' or 'register'\"}");
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"status\":\"error\", \"message\":\"" + e.getMessage().replace("\"", "'") + "\"}");
        } finally {
            out.close();
        }
    }

    private void handleLogin(HttpServletRequest request, HttpServletResponse response, PrintWriter out) throws Exception {
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
    }

    private void handleRegister(HttpServletRequest request, HttpServletResponse response, PrintWriter out) throws Exception {
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
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        response.getWriter().print("{\"error\":\"GET not supported. Use POST with ?action=login or ?action=register\"}");
    }
}
