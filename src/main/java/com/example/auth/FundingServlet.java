package com.example.auth;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import java.io.*;
import java.sql.*;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.exceptions.JWTVerificationException;

@WebServlet("/funding")
public class FundingServlet extends HttpServlet {

    private static final String SECRET = "pk1908seckeyret007";

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

       
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.println("{\"error\":\"Missing or invalid token\"}");
            return;
        }

        String token = authHeader.substring("Bearer ".length());
        String tokenEmail;

        try {
            Algorithm algorithm = Algorithm.HMAC256(SECRET);
            JWTVerifier verifier = JWT.require(algorithm).build();
            DecodedJWT jwt = verifier.verify(token);
            tokenEmail = jwt.getClaim("email").asString();
        } catch (JWTVerificationException e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.println("{\"error\":\"Invalid token\"}");
            return;
        }

        
        String email = request.getParameter("email");
        String amountParam = request.getParameter("amount");

        if (email == null || amountParam == null || !email.equals(tokenEmail)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            out.println("{\"error\":\"Email mismatch or missing parameters\"}");
            return;
        }

        int amount;
        try {
            amount = Integer.parseInt(amountParam);
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.println("{\"error\":\"Invalid amount\"}");
            return;
        }

       
        try (Connection conn = DBConnection.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement("UPDATE users SET balance = balance + ? WHERE email = ?");
            stmt.setInt(1, amount);
            stmt.setString(2, email);

            int rows = stmt.executeUpdate();

            if (rows > 0) {
                out.println("{\"status\":\"success\"}");
            } else {
                out.println("{\"status\":\"failed\"}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            out.println("{\"status\":\"error\", \"message\":\"" + e.getMessage().replace("\"", "'") + "\"}");
        }
    }
}
