package com.example.auth;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebFilter("/*")
public class AuthFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        String path = req.getRequestURI();
        System.out.println("path is " + path);

        
        if (path.contains("/login") || path.contains("/register") || req.getMethod().equalsIgnoreCase("OPTIONS")) {
            chain.doFilter(request, response);
            return;
        }

        String authHeader = req.getHeader("Authorization");

        try {
            String emailFromToken = JWTUtil.verifyTokenAndGetEmail(authHeader);

            String emailFromRequest = req.getParameter("email");

            if (emailFromRequest == null || !emailFromRequest.equalsIgnoreCase(emailFromToken)) {
                setCorsHeaders(res);
                res.setStatus(HttpServletResponse.SC_FORBIDDEN);
                res.setContentType("application/json");
                res.getWriter().write("{\"error\":\"Email in request does not match token\"}");
                return;
            }

            
            req.setAttribute("tokenEmail", emailFromToken);

            chain.doFilter(request, response);

        } catch (Exception e) {
            setCorsHeaders(res); 
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            res.setContentType("application/json");
            res.getWriter().write("{\"error\":\"" + e.getMessage().replace("\"", "'") + "\"}");
        }
    }

    private void setCorsHeaders(HttpServletResponse res) {
        res.setHeader("Access-Control-Allow-Origin", "http://localhost:4200");
        res.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        res.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        res.setHeader("Access-Control-Allow-Credentials", "true");
    }
}
