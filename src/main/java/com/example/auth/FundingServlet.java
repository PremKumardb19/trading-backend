package com.example.auth;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import java.io.*;
import java.sql.*;

@WebServlet("/funding")
public class FundingServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        String email = (String) request.getAttribute("tokenEmail");
        if (email == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.println("{\"error\":\"Unauthorized access\"}");
            return;
        }

        String amountParam = request.getParameter("amount");

        if (amountParam == null || amountParam.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.println("{\"error\":\"Missing amount parameter\"}");
            return;
        }

        int amount;
        try {
            amount = Integer.parseInt(amountParam);
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.println("{\"error\":\"Invalid amount format\"}");
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                "UPDATE users SET balance = balance + ? WHERE email = ?"
            );
            stmt.setInt(1, amount);
            stmt.setString(2, email);

            int rows = stmt.executeUpdate();

            if (rows > 0) {
                response.setStatus(HttpServletResponse.SC_OK);
                out.println("{\"status\":\"success\", \"message\":\"Funds added\"}");
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.println("{\"status\":\"failed\", \"message\":\"User not found\"}");
            }

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.println("{\"status\":\"error\", \"message\":\"" + e.getMessage().replace("\"", "'") + "\"}");
        }
    }
}
