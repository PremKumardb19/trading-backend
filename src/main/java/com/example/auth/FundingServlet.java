package com.example.auth;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import java.io.*;
import java.sql.*;

@WebServlet("/funding")
public class FundingServlet extends HttpServlet {
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        
        String email = request.getParameter("email");
        int amount = Integer.parseInt(request.getParameter("amount"));

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        try (Connection conn = DBConnection.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement("UPDATE users SET balance=balance+? WHERE email=?");
            stmt.setInt(1, amount);
            stmt.setString(2, email);
            int rows=stmt.executeUpdate();

            if (rows!=0) {
                out.println("{\"status\":\"success\"}");
            } else {
                out.println("{\"status\":\"failed\"}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            out.println("{\"status\":\"error\", \"message\":\"" + e.getMessage() + "\"}");
        }
    }
}
